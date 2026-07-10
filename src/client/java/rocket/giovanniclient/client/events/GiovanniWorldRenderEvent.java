package rocket.giovanniclient.client.events;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;
import rocket.giovanniclient.client.features.render.GlbRatModel;
import rocket.giovanniclient.client.features.render.GiovanniRenderLayers;
import rocket.giovanniclient.client.features.render.PlayerTracer;
import rocket.giovanniclient.client.features.render.RatReplacer;
import rocket.giovanniclient.client.features.slayers.blaze.BlazeShieldHighlight;

public class GiovanniWorldRenderEvent {
    private static final MultiBufferSource.BufferSource IMMEDIATE = MultiBufferSource.immediate(new ByteBufferBuilder(2048));

    private final CameraRenderState camera;
    private final PoseStack matrices;
    private final DeltaTracker tickCounter;

    public GiovanniWorldRenderEvent(CameraRenderState camera, PoseStack matrices, DeltaTracker tickCounter) {
        this.camera = camera;
        this.matrices = matrices;
        this.tickCounter = tickCounter;
    }

    public void render() {
        float tickProgress = tickCounter.getGameTimeDeltaPartialTick(true);
        for (BlazeShieldHighlight.RenderedBox box : BlazeShieldHighlight.getRenderedBoxes(tickProgress)) {
            drawFilled(box.box(), box.fillArgb());
            drawOutline(box.box(), box.outlineArgb());
        }
        for (RatReplacer.RatRenderData rat : RatReplacer.getRenderData(tickProgress)) {
            drawRat(rat);
        }
        drawPlayerTracers(tickProgress);
        IMMEDIATE.endBatch();
    }

    private void drawRat(RatReplacer.RatRenderData rat) {
        GlbRatModel.render(matrices, IMMEDIATE, rat, camera.pos);
    }

    private void drawFilled(AABB box, int color) {
        VertexConsumer consumer = IMMEDIATE.getBuffer(RenderTypes.debugFilledBox());
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;

        drawQuad(new Vec3(maxX, minY, minZ), new Vec3(maxX, maxY, minZ), new Vec3(maxX, maxY, maxZ), new Vec3(maxX, minY, maxZ), consumer, color);
        drawQuad(new Vec3(minX, minY, minZ), new Vec3(minX, minY, maxZ), new Vec3(minX, maxY, maxZ), new Vec3(minX, maxY, minZ), consumer, color);
        drawQuad(new Vec3(minX, minY, minZ), new Vec3(minX, maxY, minZ), new Vec3(maxX, maxY, minZ), new Vec3(maxX, minY, minZ), consumer, color);
        drawQuad(new Vec3(minX, minY, maxZ), new Vec3(maxX, minY, maxZ), new Vec3(maxX, maxY, maxZ), new Vec3(minX, maxY, maxZ), consumer, color);
        drawQuad(new Vec3(minX, maxY, minZ), new Vec3(minX, maxY, maxZ), new Vec3(maxX, maxY, maxZ), new Vec3(maxX, maxY, minZ), consumer, color);
        drawQuad(new Vec3(minX, minY, minZ), new Vec3(maxX, minY, minZ), new Vec3(maxX, minY, maxZ), new Vec3(minX, minY, maxZ), consumer, color);
    }

    private void drawOutline(AABB box, int color) {
        VertexConsumer consumer = IMMEDIATE.getBuffer(RenderTypes.lines());
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;

        drawLine(new Vec3(minX, minY, minZ), new Vec3(maxX, minY, minZ), consumer, color);
        drawLine(new Vec3(minX, minY, minZ), new Vec3(minX, maxY, minZ), consumer, color);
        drawLine(new Vec3(minX, minY, minZ), new Vec3(minX, minY, maxZ), consumer, color);
        drawLine(new Vec3(maxX, minY, minZ), new Vec3(maxX, maxY, minZ), consumer, color);
        drawLine(new Vec3(maxX, maxY, minZ), new Vec3(minX, maxY, minZ), consumer, color);
        drawLine(new Vec3(minX, maxY, minZ), new Vec3(minX, maxY, maxZ), consumer, color);
        drawLine(new Vec3(minX, maxY, maxZ), new Vec3(minX, minY, maxZ), consumer, color);
        drawLine(new Vec3(minX, minY, maxZ), new Vec3(maxX, minY, maxZ), consumer, color);
        drawLine(new Vec3(maxX, minY, maxZ), new Vec3(maxX, minY, minZ), consumer, color);
        drawLine(new Vec3(minX, maxY, maxZ), new Vec3(maxX, maxY, maxZ), consumer, color);
        drawLine(new Vec3(maxX, minY, maxZ), new Vec3(maxX, maxY, maxZ), consumer, color);
        drawLine(new Vec3(maxX, maxY, minZ), new Vec3(maxX, maxY, maxZ), consumer, color);
    }

    private void drawPlayerTracers(float tickProgress) {
        Minecraft client = Minecraft.getInstance();
        if (!PlayerTracer.isEnabled() || client.level == null || client.player == null) return;

        int color = 0x80FFFFFF;
        Vec3 tracerStart = camera.pos.add(Vec3.directionFromRotation(
                client.gameRenderer.getMainCamera().xRot(),
                client.gameRenderer.getMainCamera().yRot()
        ).scale(10));
        for (AbstractClientPlayer player : client.level.players()) {
            if (player == client.player || !PlayerTracer.matches(player)) continue;

            AABB rawBox = getLerpedBox(player, tickProgress);
            VertexConsumer consumer = IMMEDIATE.getBuffer(GiovanniRenderLayers.TRACER_LINES);
            drawOutline(rawBox.move(0, 0.05, 0).inflate(0.05), consumer, color);
            drawLine(tracerStart, rawBox.getCenter(), consumer, color);
        }
    }

    private void drawOutline(AABB box, VertexConsumer consumer, int color) {
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;

        drawLine(new Vec3(minX, minY, minZ), new Vec3(maxX, minY, minZ), consumer, color);
        drawLine(new Vec3(minX, minY, minZ), new Vec3(minX, maxY, minZ), consumer, color);
        drawLine(new Vec3(minX, minY, minZ), new Vec3(minX, minY, maxZ), consumer, color);
        drawLine(new Vec3(maxX, minY, minZ), new Vec3(maxX, maxY, minZ), consumer, color);
        drawLine(new Vec3(maxX, maxY, minZ), new Vec3(minX, maxY, minZ), consumer, color);
        drawLine(new Vec3(minX, maxY, minZ), new Vec3(minX, maxY, maxZ), consumer, color);
        drawLine(new Vec3(minX, maxY, maxZ), new Vec3(minX, minY, maxZ), consumer, color);
        drawLine(new Vec3(minX, minY, maxZ), new Vec3(maxX, minY, maxZ), consumer, color);
        drawLine(new Vec3(maxX, minY, maxZ), new Vec3(maxX, minY, minZ), consumer, color);
        drawLine(new Vec3(minX, maxY, maxZ), new Vec3(maxX, maxY, maxZ), consumer, color);
        drawLine(new Vec3(maxX, minY, maxZ), new Vec3(maxX, maxY, maxZ), consumer, color);
        drawLine(new Vec3(maxX, maxY, minZ), new Vec3(maxX, maxY, maxZ), consumer, color);
    }

    private AABB getLerpedBox(AbstractClientPlayer player, float tickProgress) {
        if (player.isRemoved()) return player.getBoundingBox();

        double x = lerp(tickProgress, player.xOld, player.getX());
        double y = lerp(tickProgress, player.yOld, player.getY());
        double z = lerp(tickProgress, player.zOld, player.getZ());
        return player.getBoundingBox().move(x - player.getX(), y - player.getY(), z - player.getZ());
    }

    private double lerp(float delta, double start, double end) {
        return start + delta * (end - start);
    }

    private void drawQuad(Vec3 first, Vec3 second, Vec3 third, Vec3 fourth, VertexConsumer consumer, int color) {
        PoseStack.Pose entry = matrices.last();
        Vec3 camPos = camera.pos;
        consumer.addVertex(entry, (float)(first.x() - camPos.x()), (float)(first.y() - camPos.y()), (float)(first.z() - camPos.z())).setColor(color);
        consumer.addVertex(entry, (float)(second.x() - camPos.x()), (float)(second.y() - camPos.y()), (float)(second.z() - camPos.z())).setColor(color);
        consumer.addVertex(entry, (float)(third.x() - camPos.x()), (float)(third.y() - camPos.y()), (float)(third.z() - camPos.z())).setColor(color);
        consumer.addVertex(entry, (float)(fourth.x() - camPos.x()), (float)(fourth.y() - camPos.y()), (float)(fourth.z() - camPos.z())).setColor(color);
    }

    private void drawLine(Vec3 start, Vec3 end, VertexConsumer consumer, int color) {
        PoseStack.Pose entry = matrices.last();
        Vec3 camPos = camera.pos;
        Vector4f first = new Vector4f().set(start.x() - camPos.x(), start.y() - camPos.y(), start.z() - camPos.z(), 1.0);
        Vector4f second = new Vector4f().set(end.x() - camPos.x(), end.y() - camPos.y(), end.z() - camPos.z(), 1.0);
        consumer.addVertex(entry, first.x, first.y, first.z)
                .setNormal(entry, second.x - first.x, second.y - first.y, second.z - first.z)
                .setColor(color)
                .setLineWidth(3.0f);
        consumer.addVertex(entry, second.x, second.y, second.z)
                .setNormal(entry, second.x - first.x, second.y - first.y, second.z - first.z)
                .setColor(color)
                .setLineWidth(3.0f);
    }
}
