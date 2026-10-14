package rocket.giovanniclient.client.events;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;
import rocket.giovanniclient.client.features.render.GlbRatModel;
import rocket.giovanniclient.client.features.render.GiovanniRenderLayers;
import rocket.giovanniclient.client.features.render.PlayerTracer;
import rocket.giovanniclient.client.features.render.RatReplacer;
import rocket.giovanniclient.client.features.slayers.blaze.BlazeShieldHighlight;

import java.util.ArrayList;
import java.util.List;

public class GiovanniWorldRenderEvent {
    private static final RenderStateDataKey<Frame> FRAME_KEY = RenderStateDataKey.create(() -> "Giovanni world overlays");
    private static boolean registered;

    public record Tracer(AABB outline, Vec3 start, Vec3 end, int color) {}

    /** Values only: no entities, mutable camera state or client references survive extraction. */
    public record Frame(Vec3 cameraPosition, List<BlazeShieldHighlight.RenderedBox> boxes,
                        List<RatReplacer.RatRenderData> rats, List<Tracer> tracers) {
        public static final Frame EMPTY = new Frame(Vec3.ZERO, List.of(), List.of(), List.of());

        public Frame {
            boxes = List.copyOf(boxes);
            rats = List.copyOf(rats);
            tracers = List.copyOf(tracers);
        }
    }

    private final Frame frame;
    private final PoseStack matrices;
    private final SubmitNodeCollector collector;

    public GiovanniWorldRenderEvent(Frame frame, SubmitNodeCollector collector) {
        this.frame = frame;
        this.matrices = new PoseStack();
        this.collector = collector;
    }

    public static void register() {
        if (registered) return;
        registered = true;
        LevelExtractionEvents.END_EXTRACTION.register(context -> context.levelState().setData(FRAME_KEY, extract(context)));
    }

    public static Frame frame(LevelRenderState state) {
        return state.getDataOrDefault(FRAME_KEY, Frame.EMPTY);
    }

    private static Frame extract(LevelExtractionContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return Frame.EMPTY;

        var camera = context.levelState().cameraRenderState;
        Vec3 cameraPosition = camera.pos;
        float tickProgress = context.deltaTracker().getGameTimeDeltaPartialTick(true);
        List<Tracer> tracers = new ArrayList<>();
        if (PlayerTracer.isEnabled()) {
            Vec3 start = cameraPosition.add(Vec3.directionFromRotation(camera.xRot, camera.yRot).scale(10));
            for (AbstractClientPlayer player : context.level().players()) {
                if (player == client.player || !PlayerTracer.matches(player)) continue;
                AABB box = getLerpedBox(player, tickProgress);
                tracers.add(new Tracer(box.move(0, 0.05, 0).inflate(0.05), start, box.getCenter(), 0x80FFFFFF));
            }
        }
        return new Frame(cameraPosition,
                BlazeShieldHighlight.extractRenderedBoxes(context.level(), tickProgress),
                RatReplacer.extractRenderData(context.level(), tickProgress), tracers);
    }

    private static AABB getLerpedBox(AbstractClientPlayer player, float tickProgress) {
        if (player.isRemoved()) return player.getBoundingBox();
        double x = player.xOld + tickProgress * (player.getX() - player.xOld);
        double y = player.yOld + tickProgress * (player.getY() - player.yOld);
        double z = player.zOld + tickProgress * (player.getZ() - player.zOld);
        return player.getBoundingBox().move(x - player.getX(), y - player.getY(), z - player.getZ());
    }

    public void render() {
        for (BlazeShieldHighlight.RenderedBox box : frame.boxes()) {
            drawFilled(box.box(), box.fillArgb());
            drawOutline(box.box(), box.outlineArgb());
        }
        for (RatReplacer.RatRenderData rat : frame.rats()) {
            drawRat(rat);
        }
        drawPlayerTracers();
    }

    private void drawRat(RatReplacer.RatRenderData rat) {
        GlbRatModel.submit(collector, matrices, rat, frame.cameraPosition());
    }

    private void drawFilled(AABB box, int color) {
        collector.submitCustomGeometry(matrices, RenderTypes.debugFilledBox(),
                (pose, consumer) -> drawFilled(box, pose, consumer, color));
    }

    private void drawFilled(AABB box, PoseStack.Pose pose, VertexConsumer consumer, int color) {
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;

        drawQuad(new Vec3(maxX, minY, minZ), new Vec3(maxX, maxY, minZ), new Vec3(maxX, maxY, maxZ), new Vec3(maxX, minY, maxZ), pose, consumer, color);
        drawQuad(new Vec3(minX, minY, minZ), new Vec3(minX, minY, maxZ), new Vec3(minX, maxY, maxZ), new Vec3(minX, maxY, minZ), pose, consumer, color);
        drawQuad(new Vec3(minX, minY, minZ), new Vec3(minX, maxY, minZ), new Vec3(maxX, maxY, minZ), new Vec3(maxX, minY, minZ), pose, consumer, color);
        drawQuad(new Vec3(minX, minY, maxZ), new Vec3(maxX, minY, maxZ), new Vec3(maxX, maxY, maxZ), new Vec3(minX, maxY, maxZ), pose, consumer, color);
        drawQuad(new Vec3(minX, maxY, minZ), new Vec3(minX, maxY, maxZ), new Vec3(maxX, maxY, maxZ), new Vec3(maxX, maxY, minZ), pose, consumer, color);
        drawQuad(new Vec3(minX, minY, minZ), new Vec3(maxX, minY, minZ), new Vec3(maxX, minY, maxZ), new Vec3(minX, minY, maxZ), pose, consumer, color);
    }

    private void drawOutline(AABB box, int color) {
        collector.submitCustomGeometry(matrices, RenderTypes.lines(),
                (pose, consumer) -> drawOutline(box, pose, consumer, color));
    }

    private void drawPlayerTracers() {
        for (Tracer tracer : frame.tracers()) {
            collector.submitCustomGeometry(matrices, GiovanniRenderLayers.TRACER_LINES, (pose, consumer) -> {
                drawOutline(tracer.outline(), pose, consumer, tracer.color());
                drawLine(tracer.start(), tracer.end(), pose, consumer, tracer.color());
            });
        }
    }

    private void drawOutline(AABB box, PoseStack.Pose pose, VertexConsumer consumer, int color) {
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;

        drawLine(new Vec3(minX, minY, minZ), new Vec3(maxX, minY, minZ), pose, consumer, color);
        drawLine(new Vec3(minX, minY, minZ), new Vec3(minX, maxY, minZ), pose, consumer, color);
        drawLine(new Vec3(minX, minY, minZ), new Vec3(minX, minY, maxZ), pose, consumer, color);
        drawLine(new Vec3(maxX, minY, minZ), new Vec3(maxX, maxY, minZ), pose, consumer, color);
        drawLine(new Vec3(maxX, maxY, minZ), new Vec3(minX, maxY, minZ), pose, consumer, color);
        drawLine(new Vec3(minX, maxY, minZ), new Vec3(minX, maxY, maxZ), pose, consumer, color);
        drawLine(new Vec3(minX, maxY, maxZ), new Vec3(minX, minY, maxZ), pose, consumer, color);
        drawLine(new Vec3(minX, minY, maxZ), new Vec3(maxX, minY, maxZ), pose, consumer, color);
        drawLine(new Vec3(maxX, minY, maxZ), new Vec3(maxX, minY, minZ), pose, consumer, color);
        drawLine(new Vec3(minX, maxY, maxZ), new Vec3(maxX, maxY, maxZ), pose, consumer, color);
        drawLine(new Vec3(maxX, minY, maxZ), new Vec3(maxX, maxY, maxZ), pose, consumer, color);
        drawLine(new Vec3(maxX, maxY, minZ), new Vec3(maxX, maxY, maxZ), pose, consumer, color);
    }

    private void drawQuad(Vec3 first, Vec3 second, Vec3 third, Vec3 fourth, PoseStack.Pose entry, VertexConsumer consumer, int color) {
        Vec3 camPos = frame.cameraPosition();
        consumer.addVertex(entry, (float)(first.x() - camPos.x()), (float)(first.y() - camPos.y()), (float)(first.z() - camPos.z())).setColor(color);
        consumer.addVertex(entry, (float)(second.x() - camPos.x()), (float)(second.y() - camPos.y()), (float)(second.z() - camPos.z())).setColor(color);
        consumer.addVertex(entry, (float)(third.x() - camPos.x()), (float)(third.y() - camPos.y()), (float)(third.z() - camPos.z())).setColor(color);
        consumer.addVertex(entry, (float)(fourth.x() - camPos.x()), (float)(fourth.y() - camPos.y()), (float)(fourth.z() - camPos.z())).setColor(color);
    }

    private void drawLine(Vec3 start, Vec3 end, PoseStack.Pose entry, VertexConsumer consumer, int color) {
        Vec3 camPos = frame.cameraPosition();
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
