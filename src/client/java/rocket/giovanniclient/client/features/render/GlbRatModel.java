package rocket.giovanniclient.client.features.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class GlbRatModel {
    private static final Identifier MODEL_ID = Identifier.fromNamespaceAndPath("giovanniclient", "models/entity/big_rat.glb");
    private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath("giovanniclient", "textures/entity/big_rat.png");
    private static final int CHUNK_JSON = 0x4E4F534A;
    private static final int CHUNK_BIN = 0x004E4942;
    private static final int FULL_BRIGHT = 15728880;
    private static final float MODEL_SCALE = 1.9f;

    private static GlbRatModel instance;
    private static boolean loadAttempted;

    private final List<Vertex> vertices;

    private GlbRatModel(List<Vertex> vertices) {
        this.vertices = vertices;
    }

    public static void render(PoseStack matrices, MultiBufferSource.BufferSource buffers, RatReplacer.RatRenderData rat, Vec3 cameraPosition) {
        GlbRatModel model = get();
        if (model == null) return;

        matrices.pushPose();
        matrices.translate(rat.position().x() - cameraPosition.x(), rat.position().y() - cameraPosition.y() + 1.38, rat.position().z() - cameraPosition.z());
        matrices.mulPose(new Quaternionf().rotationY((float) Math.toRadians(180.0f - rat.yRot())));
        matrices.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);

        VertexConsumer consumer = buffers.getBuffer(RenderTypes.entitySolid(TEXTURE_ID));
        PoseStack.Pose pose = matrices.last();
        for (Vertex vertex : model.vertices) {
            consumer.addVertex(pose, vertex.x, vertex.y, vertex.z)
                    .setColor(0xffffffff)
                    .setUv(vertex.u, vertex.v)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(FULL_BRIGHT)
                    .setNormal(pose, vertex.nx, vertex.ny, vertex.nz);
        }

        matrices.popPose();
    }

    private static GlbRatModel get() {
        if (!loadAttempted) {
            loadAttempted = true;
            try {
                instance = load();
            } catch (Exception e) {
                rocket.giovanniclient.client.util.Utils.error("Failed to load Giovanni rat GLB model", e);
            }
        }
        return instance;
    }

    private static GlbRatModel load() throws IOException {
        byte[] data;
        try (var stream = Minecraft.getInstance().getResourceManager().open(MODEL_ID)) {
            data = stream.readAllBytes();
        }
        ByteBuffer glb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        if (glb.getInt() != 0x46546C67) {
            throw new IOException("Invalid GLB magic");
        }

        glb.getInt();
        glb.getInt();

        JsonObject json = null;
        byte[] binary = null;
        while (glb.remaining() >= 8) {
            int chunkLength = glb.getInt();
            int chunkType = glb.getInt();
            byte[] chunk = new byte[chunkLength];
            glb.get(chunk);

            if (chunkType == CHUNK_JSON) {
                json = JsonParser.parseString(new String(chunk, StandardCharsets.UTF_8)).getAsJsonObject();
            } else if (chunkType == CHUNK_BIN) {
                binary = chunk;
            }
        }

        if (json == null || binary == null) {
            throw new IOException("Missing GLB JSON or BIN chunk");
        }

        ModelData modelData = new ModelData(json, binary);
        List<Vertex> vertices = new ArrayList<>();
        JsonArray scenes = json.getAsJsonArray("scenes");
        int sceneIndex = json.has("scene") ? json.get("scene").getAsInt() : 0;
        JsonArray rootNodes = scenes.get(sceneIndex).getAsJsonObject().getAsJsonArray("nodes");
        for (int i = 0; i < rootNodes.size(); i++) {
            loadNode(modelData, rootNodes.get(i).getAsInt(), identity(), vertices);
        }

        normalize(vertices);
        return new GlbRatModel(vertices);
    }

    private static void loadNode(ModelData modelData, int nodeIndex, double[] parentMatrix, List<Vertex> vertices) {
        JsonObject node = modelData.nodes().get(nodeIndex).getAsJsonObject();
        double[] matrix = multiply(parentMatrix, readNodeMatrix(node));

        if (node.has("mesh")) {
            loadMesh(modelData, node.get("mesh").getAsInt(), matrix, vertices);
        }

        if (node.has("children")) {
            JsonArray children = node.getAsJsonArray("children");
            for (int i = 0; i < children.size(); i++) {
                loadNode(modelData, children.get(i).getAsInt(), matrix, vertices);
            }
        }
    }

    private static void loadMesh(ModelData modelData, int meshIndex, double[] matrix, List<Vertex> vertices) {
        JsonObject mesh = modelData.meshes().get(meshIndex).getAsJsonObject();
        JsonArray primitives = mesh.getAsJsonArray("primitives");
        for (int i = 0; i < primitives.size(); i++) {
            JsonObject primitive = primitives.get(i).getAsJsonObject();
            if (primitive.has("mode") && primitive.get("mode").getAsInt() != 4) continue;

            JsonObject attributes = primitive.getAsJsonObject("attributes");
            Accessor positions = modelData.accessor(attributes.get("POSITION").getAsInt());
            Accessor normals = modelData.accessor(attributes.get("NORMAL").getAsInt());
            Accessor uvs = modelData.accessor(attributes.get("TEXCOORD_0").getAsInt());
            Accessor indices = modelData.accessor(primitive.get("indices").getAsInt());

            for (int index = 0; index < indices.count(); index++) {
                int vertexIndex = indices.readInt(index);
                double[] position = positions.readVec(vertexIndex);
                double[] normal = normals.readVec(vertexIndex);
                double[] uv = uvs.readVec(vertexIndex);
                double[] transformedPosition = transformPosition(matrix, position);
                double[] transformedNormal = transformNormal(matrix, normal);
                vertices.add(new Vertex(
                        (float) transformedPosition[0], (float) transformedPosition[1], (float) transformedPosition[2],
                        (float) uv[0], 1.0f - (float) uv[1],
                        (float) transformedNormal[0], (float) transformedNormal[1], (float) transformedNormal[2]
                ));
            }
        }
    }

    private static void normalize(List<Vertex> vertices) {
        if (vertices.isEmpty()) return;

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxZ = -Float.MAX_VALUE;
        for (Vertex vertex : vertices) {
            minX = Math.min(minX, vertex.x);
            minY = Math.min(minY, vertex.y);
            minZ = Math.min(minZ, vertex.z);
            maxX = Math.max(maxX, vertex.x);
            maxZ = Math.max(maxZ, vertex.z);
        }

        float centerX = (minX + maxX) * 0.5f;
        float centerZ = (minZ + maxZ) * 0.5f;
        for (Vertex vertex : vertices) {
            vertex.x -= centerX;
            vertex.y -= minY;
            vertex.z -= centerZ;
        }
    }

    private static double[] identity() {
        return new double[] {
                1.0, 0.0, 0.0, 0.0,
                0.0, 1.0, 0.0, 0.0,
                0.0, 0.0, 1.0, 0.0,
                0.0, 0.0, 0.0, 1.0
        };
    }

    private static double[] readNodeMatrix(JsonObject node) {
        if (node.has("matrix")) {
            JsonArray matrix = node.getAsJsonArray("matrix");
            double[] result = new double[16];
            for (int i = 0; i < 16; i++) {
                result[i] = matrix.get(i).getAsDouble();
            }
            return result;
        }

        double[] translation = readVec(node, "translation", 0.0, 0.0, 0.0);
        double[] rotation = readVec(node, "rotation", 0.0, 0.0, 0.0, 1.0);
        double[] scale = readVec(node, "scale", 1.0, 1.0, 1.0);
        return composeMatrix(translation, rotation, scale);
    }

    private static double[] readVec(JsonObject object, String key, double... fallback) {
        if (!object.has(key)) return fallback;

        JsonArray array = object.getAsJsonArray(key);
        double[] result = new double[fallback.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = array.get(i).getAsDouble();
        }
        return result;
    }

    private static double[] composeMatrix(double[] translation, double[] rotation, double[] scale) {
        double x = rotation[0];
        double y = rotation[1];
        double z = rotation[2];
        double w = rotation[3];
        double length = Math.sqrt(x * x + y * y + z * z + w * w);
        if (length > 1.0E-6) {
            x /= length;
            y /= length;
            z /= length;
            w /= length;
        }

        double xx = x * x;
        double yy = y * y;
        double zz = z * z;
        double xy = x * y;
        double xz = x * z;
        double yz = y * z;
        double wx = w * x;
        double wy = w * y;
        double wz = w * z;

        return new double[] {
                (1.0 - 2.0 * (yy + zz)) * scale[0],
                (2.0 * (xy + wz)) * scale[0],
                (2.0 * (xz - wy)) * scale[0],
                0.0,
                (2.0 * (xy - wz)) * scale[1],
                (1.0 - 2.0 * (xx + zz)) * scale[1],
                (2.0 * (yz + wx)) * scale[1],
                0.0,
                (2.0 * (xz + wy)) * scale[2],
                (2.0 * (yz - wx)) * scale[2],
                (1.0 - 2.0 * (xx + yy)) * scale[2],
                0.0,
                translation[0],
                translation[1],
                translation[2],
                1.0
        };
    }

    private static double[] multiply(double[] a, double[] b) {
        double[] result = new double[16];
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                result[col * 4 + row] =
                        a[row] * b[col * 4]
                                + a[4 + row] * b[col * 4 + 1]
                                + a[8 + row] * b[col * 4 + 2]
                                + a[12 + row] * b[col * 4 + 3];
            }
        }
        return result;
    }

    private static double[] transformPosition(double[] matrix, double[] vec) {
        double x = vec[0];
        double y = vec[1];
        double z = vec[2];
        return new double[] {
                matrix[0] * x + matrix[4] * y + matrix[8] * z + matrix[12],
                matrix[1] * x + matrix[5] * y + matrix[9] * z + matrix[13],
                matrix[2] * x + matrix[6] * y + matrix[10] * z + matrix[14]
        };
    }

    private static double[] transformNormal(double[] matrix, double[] vec) {
        double x = matrix[0] * vec[0] + matrix[4] * vec[1] + matrix[8] * vec[2];
        double y = matrix[1] * vec[0] + matrix[5] * vec[1] + matrix[9] * vec[2];
        double z = matrix[2] * vec[0] + matrix[6] * vec[1] + matrix[10] * vec[2];
        double length = Math.sqrt(x * x + y * y + z * z);
        if (length < 1.0E-6) return new double[] {0.0, 1.0, 0.0};
        return new double[] {x / length, y / length, z / length};
    }

    private record ModelData(JsonObject json, byte[] binary) {
        private JsonArray accessors() { return json.getAsJsonArray("accessors"); }
        private JsonArray bufferViews() { return json.getAsJsonArray("bufferViews"); }
        private JsonArray meshes() { return json.getAsJsonArray("meshes"); }
        private JsonArray nodes() { return json.getAsJsonArray("nodes"); }

        private Accessor accessor(int index) {
            return new Accessor(this, accessors().get(index).getAsJsonObject());
        }
    }

    private record Accessor(ModelData modelData, JsonObject accessor) {
        private int count() { return accessor.get("count").getAsInt(); }

        private double[] readVec(int index) {
            int components = switch (accessor.get("type").getAsString()) {
                case "VEC2" -> 2;
                case "VEC3" -> 3;
                case "VEC4" -> 4;
                default -> 1;
            };

            ByteBuffer buffer = buffer(index).order(ByteOrder.LITTLE_ENDIAN);
            double[] result = new double[components];
            for (int i = 0; i < components; i++) {
                result[i] = switch (accessor.get("componentType").getAsInt()) {
                    case 5126 -> buffer.getFloat();
                    case 5125 -> Integer.toUnsignedLong(buffer.getInt());
                    case 5123 -> Short.toUnsignedInt(buffer.getShort());
                    case 5121 -> Byte.toUnsignedInt(buffer.get());
                    default -> 0.0;
                };
            }
            return result;
        }

        private int readInt(int index) {
            ByteBuffer buffer = buffer(index).order(ByteOrder.LITTLE_ENDIAN);
            return switch (accessor.get("componentType").getAsInt()) {
                case 5125 -> buffer.getInt();
                case 5123 -> Short.toUnsignedInt(buffer.getShort());
                case 5121 -> Byte.toUnsignedInt(buffer.get());
                default -> 0;
            };
        }

        private ByteBuffer buffer(int index) {
            JsonObject bufferView = modelData.bufferViews().get(accessor.get("bufferView").getAsInt()).getAsJsonObject();
            int componentSize = componentSize(accessor.get("componentType").getAsInt());
            int components = componentCount(accessor.get("type").getAsString());
            int elementSize = componentSize * components;
            int stride = bufferView.has("byteStride") ? bufferView.get("byteStride").getAsInt() : elementSize;
            int bufferViewOffset = bufferView.has("byteOffset") ? bufferView.get("byteOffset").getAsInt() : 0;
            int accessorOffset = accessor.has("byteOffset") ? accessor.get("byteOffset").getAsInt() : 0;
            int offset = bufferViewOffset + accessorOffset + index * stride;
            return ByteBuffer.wrap(modelData.binary(), offset, elementSize);
        }

        private static int componentSize(int componentType) {
            return switch (componentType) {
                case 5126, 5125 -> 4;
                case 5123 -> 2;
                case 5121 -> 1;
                default -> 4;
            };
        }

        private static int componentCount(String type) {
            return switch (type) {
                case "VEC2" -> 2;
                case "VEC3" -> 3;
                case "VEC4" -> 4;
                default -> 1;
            };
        }
    }

    private static final class Vertex {
        private float x;
        private float y;
        private float z;
        private final float u;
        private final float v;
        private final float nx;
        private final float ny;
        private final float nz;

        private Vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.u = u;
            this.v = v;
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
        }
    }
}
