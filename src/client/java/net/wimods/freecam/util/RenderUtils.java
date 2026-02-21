/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam.util;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wimods.freecam.WiFreecam;
import net.wimods.freecam.WurstRenderLayers;
import org.joml.Matrix3x2f;
import org.joml.Vector3f;

public enum RenderUtils
{
	;
	
	public static void applyRenderOffset(MatrixStack matrixStack)
	{
		Vec3d camPos = getCameraPos();
		matrixStack.translate(-camPos.x, -camPos.y, -camPos.z);
	}
	
	public static Vec3d getCameraPos()
	{
		Camera camera = WiFreecam.MC.gameRenderer.getCamera();
		if(camera == null)
			return Vec3d.ZERO;
		
		return camera.getCameraPos();
	}
	
	public static Rotation getCameraRotation()
	{
		Camera camera = WiFreecam.MC.gameRenderer.getCamera();
		if(camera == null)
			return new Rotation(0, 0);
		
		return new Rotation(camera.getYaw(), camera.getPitch());
	}
	
	public static BlockPos getCameraBlockPos()
	{
		Camera camera = WiFreecam.MC.gameRenderer.getCamera();
		if(camera == null)
			return BlockPos.ORIGIN;
		
		return camera.getBlockPos();
	}
	
	public static VertexConsumerProvider.Immediate getVCP()
	{
		return WiFreecam.MC.getBufferBuilders().getEntityVertexConsumers();
	}
	
	public static int toIntColor(float[] rgb, float opacity)
	{
		return (int)(MathHelper.clamp(opacity, 0, 1) * 255) << 24
			| (int)(MathHelper.clamp(rgb[0], 0, 1) * 255) << 16
			| (int)(MathHelper.clamp(rgb[1], 0, 1) * 255) << 8
			| (int)(MathHelper.clamp(rgb[2], 0, 1) * 255);
	}
	
	public static void drawLine(MatrixStack matrices, Vec3d start, Vec3d end,
		int color, boolean depthTest)
	{
		VertexConsumerProvider.Immediate vcp = getVCP();
		RenderLayer layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3d offset = getCameraPos().negate();
		drawLine(matrices, buffer, start.add(offset), end.add(offset), color);
		
		vcp.draw(layer);
	}
	
	private static Vec3d getTracerOrigin(float partialTicks)
	{
		return getCameraRotation().toLookVec().multiply(10);
	}
	
	public static void drawTracer(MatrixStack matrices, float partialTicks,
		Vec3d end, int color, boolean depthTest)
	{
		VertexConsumerProvider.Immediate vcp = getVCP();
		RenderLayer layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3d start = getTracerOrigin(partialTicks);
		Vec3d offset = getCameraPos().negate();
		drawLine(matrices, buffer, start, end.add(offset), color);
		
		vcp.draw(layer);
	}
	
	public static void drawLine(MatrixStack matrices, VertexConsumer buffer,
		Vec3d start, Vec3d end, int color)
	{
		MatrixStack.Entry entry = matrices.peek();
		float x1 = (float)start.x;
		float y1 = (float)start.y;
		float z1 = (float)start.z;
		float x2 = (float)end.x;
		float y2 = (float)end.y;
		float z2 = (float)end.z;
		drawLine(entry, buffer, x1, y1, z1, x2, y2, z2, color);
	}
	
	public static void drawLine(MatrixStack.Entry entry, VertexConsumer buffer,
		float x1, float y1, float z1, float x2, float y2, float z2, int color)
	{
		Vector3f normal = new Vector3f(x2, y2, z2).sub(x1, y1, z1).normalize();
		buffer.vertex(entry, x1, y1, z1).color(color)
			.normal(entry, normal).lineWidth(2);
		
		// If the line goes through the screen, add another vertex there. This
		// works around a bug in Minecraft's line shader.
		float t = new Vector3f(x1, y1, z1).negate().dot(normal);
		float length = new Vector3f(x2, y2, z2).sub(x1, y1, z1).length();
		if(t > 0 && t < length)
		{
			Vector3f closeToCam = new Vector3f(normal).mul(t).add(x1, y1, z1);
			buffer.vertex(entry, closeToCam).color(color)
				.normal(entry, normal).lineWidth(2);
			buffer.vertex(entry, closeToCam).color(color)
				.normal(entry, normal).lineWidth(2);
		}
		
		buffer.vertex(entry, x2, y2, z2).color(color)
			.normal(entry, normal).lineWidth(2);
	}
	
	public static void drawLine(VertexConsumer buffer, float x1, float y1,
		float z1, float x2, float y2, float z2, int color)
	{
		Vector3f n = new Vector3f(x2, y2, z2).sub(x1, y1, z1).normalize();
		buffer.vertex(x1, y1, z1).color(color).normal(n.x, n.y, n.z).lineWidth(2);
		buffer.vertex(x2, y2, z2).color(color).normal(n.x, n.y, n.z).lineWidth(2);
	}
	
	public static void drawOutlinedBox(MatrixStack matrices, Box box, int color,
                                       boolean depthTest)
	{
		VertexConsumerProvider.Immediate vcp = getVCP();
		RenderLayer layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		drawOutlinedBox(matrices, buffer, box.offset(getCameraPos().negate()),
			color);
		
		vcp.draw(layer);
	}
	
	public static void drawOutlinedBox(VertexConsumer buffer, Box box,
		int color)
	{
		drawOutlinedBox(new MatrixStack(), buffer, box, color);
	}
	
	public static void drawOutlinedBox(MatrixStack matrices,
		VertexConsumer buffer, Box box, int color)
	{
		MatrixStack.Entry entry = matrices.peek();
		float x1 = (float)box.minX;
		float y1 = (float)box.minY;
		float z1 = (float)box.minZ;
		float x2 = (float)box.maxX;
		float y2 = (float)box.maxY;
		float z2 = (float)box.maxZ;
		
		// bottom lines
		buffer.vertex(entry, x1, y1, z1).color(color)
			.normal(entry, 1, 0, 0).lineWidth(2);;
		buffer.vertex(entry, x2, y1, z1).color(color)
			.normal(entry, 1, 0, 0).lineWidth(2);;
		buffer.vertex(entry, x1, y1, z1).color(color)
			.normal(entry, 0, 0, 1).lineWidth(2);;
		buffer.vertex(entry, x1, y1, z2).color(color)
			.normal(entry, 0, 0, 1).lineWidth(2);;
		buffer.vertex(entry, x2, y1, z1).color(color)
			.normal(entry, 0, 0, 1).lineWidth(2);;
		buffer.vertex(entry, x2, y1, z2).color(color)
			.normal(entry, 0, 0, 1).lineWidth(2);;
		buffer.vertex(entry, x1, y1, z2).color(color)
			.normal(entry, 1, 0, 0).lineWidth(2);;
		buffer.vertex(entry, x2, y1, z2).color(color)
			.normal(entry, 1, 0, 0).lineWidth(2);;
		
		// top lines
		buffer.vertex(entry, x1, y2, z1).color(color)
			.normal(entry, 1, 0, 0).lineWidth(2);;
		buffer.vertex(entry, x2, y2, z1).color(color)
			.normal(entry, 1, 0, 0).lineWidth(2);;
		buffer.vertex(entry, x1, y2, z1).color(color)
			.normal(entry, 0, 0, 1).lineWidth(2);;
		buffer.vertex(entry, x1, y2, z2).color(color)
			.normal(entry, 0, 0, 1).lineWidth(2);;
		buffer.vertex(entry, x2, y2, z1).color(color)
			.normal(entry, 0, 0, 1).lineWidth(2);;
		buffer.vertex(entry, x2, y2, z2).color(color)
			.normal(entry, 0, 0, 1).lineWidth(2);;
		buffer.vertex(entry, x1, y2, z2).color(color)
			.normal(entry, 1, 0, 0).lineWidth(2);;
		buffer.vertex(entry, x2, y2, z2).color(color)
			.normal(entry, 1, 0, 0).lineWidth(2);;
		
		// side lines
		buffer.vertex(entry, x1, y1, z1).color(color)
			.normal(entry, 0, 1, 0).lineWidth(2);;
		buffer.vertex(entry, x1, y2, z1).color(color)
			.normal(entry, 0, 1, 0).lineWidth(2);;
		buffer.vertex(entry, x2, y1, z1).color(color)
			.normal(entry, 0, 1, 0).lineWidth(2);;
		buffer.vertex(entry, x2, y2, z1).color(color)
			.normal(entry, 0, 1, 0).lineWidth(2);;
		buffer.vertex(entry, x1, y1, z2).color(color)
			.normal(entry, 0, 1, 0).lineWidth(2);;
		buffer.vertex(entry, x1, y2, z2).color(color)
			.normal(entry, 0, 1, 0).lineWidth(2);;
		buffer.vertex(entry, x2, y1, z2).color(color)
			.normal(entry, 0, 1, 0).lineWidth(2);;
		buffer.vertex(entry, x2, y2, z2).color(color)
			.normal(entry, 0, 1, 0).lineWidth(2);;
	}
	
	/**
	 * Similar to {@link DrawContext#fill(int, int, int, int, int)}, but uses
	 * floating-point coordinates instead of integers.
	 */
	public static void fill2D(DrawContext context, float x1, float y1, float x2, float y2, int color)
	{
		int scale = WiFreecam.MC.getWindow().getScaleFactor();
		int xs1 = (int)(x1 * scale);
		int ys1 = (int)(y1 * scale);
		int xs2 = (int)(x2 * scale);
		int ys2 = (int)(y2 * scale);
		
		context.getMatrices().pushMatrix();
		context.getMatrices().scale(1F / scale);
		context.fill(xs1, ys1, xs2, ys2, color);
		context.getMatrices().popMatrix();
	}

	public static void drawLine2D(DrawContext context, float x1, float y1,
		float x2, float y2, int color)
	{
		int scale = WiFreecam.MC.getWindow().getScaleFactor();
		float x = x1 * scale;
		float y = y1 * scale;
		float w = (x2 - x1) * scale;
		float h = (y2 - y1) * scale;
		float angle = (float)MathHelper.atan2(h, w);
		int length = Math.round(MathHelper.sqrt(w * w + h * h));
		
		context.getMatrices().pushMatrix();
		context.getMatrices().scale(1F / scale);
		context.getMatrices().translate(x, y);
		context.getMatrices().rotate(angle);
		context.getMatrices().translate(-0.5F, -0.5F);
		context.drawHorizontalLine(0, length - 1, 0, color);
		context.getMatrices().popMatrix();
	}
	
	/**
	 * Similar to {@link DrawContext#drawBorder(int, int, int, int, int)}, but
	 * uses floating-point coordinates instead of integers, and is one actual
	 * pixel wide instead of one scaled pixel.
	 */
	public static void drawBorder2D(DrawContext context, float x1, float y1,
		float x2, float y2, int color)
	{
		int scale = WiFreecam.MC.getWindow().getScaleFactor();
		int x = (int)(x1 * scale);
		int y = (int)(y1 * scale);
		int w = (int)((x2 - x1) * scale);
		int h = (int)((y2 - y1) * scale);
		
		context.getMatrices().pushMatrix();
		context.getMatrices().scale(1F / scale);
		context.drawHorizontalLine(x, x + w - 1, y, color);
		context.drawHorizontalLine(x, x + w - 1, y + h - 1, color);
		context.drawVerticalLine(x, y + 1, y + h - 1, color);
		context.drawVerticalLine(x + w - 1, y + 1, y + h - 1, color);
		context.getMatrices().popMatrix();
	}
	
	/**
	 * Draws a 1px border around the given polygon.
	 */
	public static void drawLineStrip2D(DrawContext context, float[][] vertices,
		int color)
	{
		if(vertices.length < 2)
			return;
		
		for(int i = 1; i < vertices.length; i++)
			drawLine2D(context, vertices[i - 1][0], vertices[i - 1][1],
				vertices[i][0], vertices[i][1], color);
		drawLine2D(context, vertices[vertices.length - 1][0],
			vertices[vertices.length - 1][1], vertices[0][0], vertices[0][1],
			color);
	}
}
