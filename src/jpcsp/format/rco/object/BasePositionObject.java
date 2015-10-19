/*
This file is part of jpcsp.

Jpcsp is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Jpcsp is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
 */
package jpcsp.format.rco.object;

import static java.lang.Math.round;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import jpcsp.format.RCO.RCOEntry;
import jpcsp.format.rco.IDisplay;
import jpcsp.format.rco.ObjectField;
import jpcsp.format.rco.anim.AbstractAnimAction;
import jpcsp.format.rco.type.EventType;
import jpcsp.format.rco.type.FloatType;
import jpcsp.format.rco.type.IntType;
import jpcsp.format.rco.vsmx.interpreter.VSMXArray;
import jpcsp.format.rco.vsmx.interpreter.VSMXBaseObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXInterpreter;
import jpcsp.format.rco.vsmx.interpreter.VSMXNativeObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXNumber;
import jpcsp.format.rco.vsmx.objects.BaseNativeObject;
import jpcsp.format.rco.vsmx.objects.Resource;

public class BasePositionObject extends BaseObject implements IDisplay {
	@ObjectField(order = 101)
	public FloatType posX;
	@ObjectField(order = 102)
	public FloatType posY;
	@ObjectField(order = 103)
	public FloatType posZ;
	@ObjectField(order = 104)
	public FloatType redScale;
	@ObjectField(order = 105)
	public FloatType greenScale;
	@ObjectField(order = 106)
	public FloatType blueScale;
	@ObjectField(order = 107)
	public FloatType alphaScale;
	@ObjectField(order = 108)
	public FloatType width;
	@ObjectField(order = 109)
	public FloatType height;
	@ObjectField(order = 110)
	public FloatType depth;
	@ObjectField(order = 111)
	public FloatType scaleWidth;
	@ObjectField(order = 112)
	public FloatType scaleHeight;
	@ObjectField(order = 113)
	public FloatType scaleDepth;
	@ObjectField(order = 114)
	public IntType iconOffset;
	@ObjectField(order = 115)
	public EventType onInit;

	public float rotateX;
	public float rotateY;
	public float rotateAngle;
	public float animX;
	public float animY;
	public float animZ;

	private class AnimRotateAction extends AbstractAnimAction {
		private float angle;

		public AnimRotateAction(float x, float y, float angle, int duration) {
			super(duration);
			rotateX = x;
			rotateY = y;
			this.angle = angle;
		}

		@Override
		protected void anim(float step) {
			rotateAngle = angle * step;
			if (log.isDebugEnabled()) {
				log.debug(String.format("AnimRotateAction to angle=%f", rotateAngle));
			}
		}
	}

	private class AnimPosAction extends AbstractAnimAction {
		private float x;
		private float y;
		private float z;
		private float startX;
		private float startY;
		private float startZ;

		public AnimPosAction(float x, float y, float z, int duration) {
			super(duration);
			this.x = x;
			this.y = y;
			this.z = z;

			startX = posX.getFloatValue();
			startY = posY.getFloatValue();
			startZ = posZ.getFloatValue();
		}

		@Override
		protected void anim(float step) {
			posX.setFloatValue(interpolate(startX, x, step));
			posY.setFloatValue(interpolate(startY, y, step));
			posZ.setFloatValue(interpolate(startZ, z, step));

			if (log.isDebugEnabled()) {
				log.debug(String.format("AnimPosAction from (%f,%f,%f) to (%f,%f,%f)", startX, startY, startZ, posX.getFloatValue(), posY.getFloatValue(), posZ.getFloatValue()));
			}
		}
	}

	private class AnimScaleAction extends AbstractAnimAction {
		private float width;
		private float height;
		private float depth;
		private float startWidth;
		private float startHeight;
		private float startDepth;

		public AnimScaleAction(float width, float height, float depth, int duration) {
			super(duration);
			this.width = width;
			this.height = height;
			this.depth = depth;

			startWidth = scaleWidth.getFloatValue();
			startHeight = scaleHeight.getFloatValue();
			startDepth = scaleDepth.getFloatValue();
		}

		@Override
		protected void anim(float step) {
			scaleWidth.setFloatValue(interpolate(startWidth, width, step));
			scaleHeight.setFloatValue(interpolate(startHeight, height, step));
			scaleDepth.setFloatValue(interpolate(startDepth, depth, step));

			if (log.isDebugEnabled()) {
				log.debug(String.format("AnimScaleAction scaling from (%f,%f,%f) to (%f,%f,%f)", startWidth, startHeight, startDepth, scaleWidth.getFloatValue(), scaleHeight.getFloatValue(), scaleDepth.getFloatValue()));
			}
		}
	}

	@Override
	public int getWidth() {
		return Math.round(width.getFloatValue() * scaleWidth.getFloatValue());
	}

	@Override
	public int getHeight() {
		return Math.round(height.getFloatValue() * scaleHeight.getFloatValue());
	}

	@Override
	public BufferedImage getImage() {
		BufferedImage image = null;

		if (getObject().hasPropertyValue(Resource.textureName)) {
			VSMXBaseObject textureObject = getObject().getPropertyValue(Resource.textureName);
			if (textureObject instanceof VSMXNativeObject) {
				BaseNativeObject texture = ((VSMXNativeObject) textureObject).getObject();
				if (texture instanceof ImageObject) {
					image = ((ImageObject) texture).getImage();
				}
			}
		}

		return image;
	}

	@Override
	public int getX() {
		return posX.getIntValue() + round(animX);
	}

	@Override
	public int getY() {
		return posY.getIntValue() + round(animY);
	}

	@Override
	public BufferedImage getAnimImage() {
		BufferedImage image = getImage();
		if (image == null || rotateAngle == 0f) {
			return image;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("Rotating image at (%f,%f) by %f", rotateX, rotateY, rotateAngle));
		}
		AffineTransform rotation = new AffineTransform();
		rotation.rotate(-rotateAngle, rotateX + image.getWidth() / 2, rotateY + image.getHeight() / 2);
		AffineTransformOp op = new AffineTransformOp(rotation, AffineTransformOp.TYPE_BILINEAR);

		return op.filter(image, null);
	}

	public void setPos(VSMXBaseObject object, VSMXBaseObject posX, VSMXBaseObject posY) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setPos(%s, %s)", posX, posY));
		}
		this.posX.setFloatValue(posX.getFloatValue());
		this.posY.setFloatValue(posY.getFloatValue());
	}

	public void setPos(VSMXBaseObject object, VSMXBaseObject posX, VSMXBaseObject posY, VSMXBaseObject posZ) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setPos(%s, %s, %s)", posX, posY, posZ));
		}
		this.posX.setFloatValue(posX.getFloatValue());
		this.posY.setFloatValue(posY.getFloatValue());
		this.posZ.setFloatValue(posZ.getFloatValue());
	}

	public VSMXBaseObject getPos(VSMXBaseObject object) {
		VSMXInterpreter interpreter = object.getInterpreter();
		VSMXArray pos = new VSMXArray(interpreter, 3);
		pos.setPropertyValue(0, new VSMXNumber(interpreter, posX.getFloatValue()));
		pos.setPropertyValue(1, new VSMXNumber(interpreter, posY.getFloatValue()));
		pos.setPropertyValue(2, new VSMXNumber(interpreter, posZ.getFloatValue()));

		if (log.isDebugEnabled()) {
			log.debug(String.format("getPos() returning %s", pos));
		}

		return pos;
	}

	public void setRotate(VSMXBaseObject object, VSMXBaseObject x, VSMXBaseObject y, VSMXBaseObject rotationRads) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setRotate(%s, %s, %s)", x, y, rotationRads));
		}

		rotateX = x.getFloatValue();
		rotateY = y.getFloatValue();
		rotateAngle = rotationRads.getFloatValue();
	}

	public VSMXBaseObject getColor(VSMXBaseObject object) {
		VSMXInterpreter interpreter = object.getInterpreter();
		VSMXArray color = new VSMXArray(interpreter, 4);
		color.setPropertyValue(0, new VSMXNumber(interpreter, redScale.getFloatValue()));
		color.setPropertyValue(1, new VSMXNumber(interpreter, greenScale.getFloatValue()));
		color.setPropertyValue(2, new VSMXNumber(interpreter, blueScale.getFloatValue()));
		color.setPropertyValue(3, new VSMXNumber(interpreter, alphaScale.getFloatValue()));

		if (log.isDebugEnabled()) {
			log.debug(String.format("getColor() returning %s", color));
		}

		return color;
	}

	public void setColor(VSMXBaseObject object, VSMXBaseObject red, VSMXBaseObject green, VSMXBaseObject blue, VSMXBaseObject alpha) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setColor(%s, %s, %s, %s)", red, green, blue, alpha));
		}

		redScale.setFloatValue(red.getFloatValue());
		greenScale.setFloatValue(green.getFloatValue());
		blueScale.setFloatValue(blue.getFloatValue());
		alphaScale.setFloatValue(alpha.getFloatValue());
	}

	public void animColor(VSMXBaseObject object, VSMXBaseObject red, VSMXBaseObject green, VSMXBaseObject blue, VSMXBaseObject alpha, VSMXBaseObject duration) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("animColor(%s, %s, %s, %s, %s)", red, green, blue, alpha, duration));
		}
	}

	public void setScale(VSMXBaseObject object, VSMXBaseObject width, VSMXBaseObject height, VSMXBaseObject depth) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setScale(%s, %s, %s)", width, height, depth));
		}
	}

	public void animScale(VSMXBaseObject object, VSMXBaseObject width, VSMXBaseObject height, VSMXBaseObject depth, VSMXBaseObject duration) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("animScale(%s, %s, %s, %s)", width, height, depth, duration));
		}

		AnimScaleAction action = new AnimScaleAction(width.getFloatValue(), height.getFloatValue(), depth.getFloatValue(), duration.getIntValue());
		getScheduler().addAction(action);
	}

	public void animPos(VSMXBaseObject object, VSMXBaseObject x, VSMXBaseObject y, VSMXBaseObject z, VSMXBaseObject duration) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("animPos from (%s,%s,%s) to (%s, %s, %s), duration=%s", posX, posY, posZ, x, y, z, duration));
		}

		AnimPosAction action = new AnimPosAction(x.getFloatValue(), y.getFloatValue(), z.getFloatValue(), duration.getIntValue());
		getScheduler().addAction(action);
	}

	public void animRotate(VSMXBaseObject object, VSMXBaseObject x, VSMXBaseObject y, VSMXBaseObject rotationRads, VSMXBaseObject duration) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("animRotate(%s, %s, %s, %s)", x, y, rotationRads, duration));
		}

		AnimRotateAction action = new AnimRotateAction(x.getFloatValue(), y.getFloatValue(), rotationRads.getFloatValue(), duration.getIntValue());
		getScheduler().addAction(action);
	}

	public void setFocus(VSMXBaseObject object) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setFocus()"));
		}
		if (display != null) {
			display.setFocus(getObject());
		}
		if (controller != null) {
			controller.setFocus(this);
		}
	}

	public void setSize(VSMXBaseObject object, VSMXBaseObject width, VSMXBaseObject height, VSMXBaseObject depth) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setSize(%s, %s, %s)", width, height, depth));
		}

		this.width.setFloatValue(width.getFloatValue());
		this.height.setFloatValue(height.getFloatValue());
		this.depth.setFloatValue(depth.getFloatValue());
	}

	public void onUp() {
	}

	public void onDown() {
	}

	public void onLeft() {
	}

	public void onRight() {
	}

	public void onPush() {
	}

	@Override
	public VSMXBaseObject createVSMXObject(VSMXInterpreter interpreter, VSMXBaseObject parent, RCOEntry entry) {
		VSMXBaseObject object = super.createVSMXObject(interpreter, parent, entry);

		BufferedImage image = getImage();
		if (image != null) {
			object.setPropertyValue(Resource.textureName, new VSMXNativeObject(interpreter, new ImageObject(image)));
		}

		return object;
	}
}
