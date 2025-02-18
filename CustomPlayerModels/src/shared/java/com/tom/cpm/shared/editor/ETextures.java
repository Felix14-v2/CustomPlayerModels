package com.tom.cpm.shared.editor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.tom.cpl.util.Image;
import com.tom.cpm.shared.editor.anim.AnimatedTex;
import com.tom.cpm.shared.editor.tree.TreeElement;
import com.tom.cpm.shared.io.IOHelper;
import com.tom.cpm.shared.model.TextureSheetType;
import com.tom.cpm.shared.util.TextureStitcher;

public class ETextures implements TreeElement {
	public static final int MAX_TEX_SIZE = 8192;
	private final Editor e;
	private TextureSheetType type;
	public EditorTexture provider;
	private EditorTexture renderTexture;
	public TextureStitcher stitcher;
	public Consumer<TextureStitcher> textureLoader;
	public File file;
	private Image defaultImg;
	public List<AnimatedTex> animatedTexs = new ArrayList<>();
	private final AnimTreeList animsList = new AnimTreeList();

	public ETextures(Editor e, TextureSheetType type) {
		this(e, type, (Consumer<TextureStitcher>) null);
	}

	public ETextures(Editor e, TextureSheetType type, Consumer<TextureStitcher> textureLoader) {
		this(e, new EditorTexture());
		this.type = type;
		this.textureLoader = textureLoader;
		if(textureLoader != null) {
			renderTexture = new EditorTexture();
			stitcher = new TextureStitcher(MAX_TEX_SIZE);
		} else if(type.editable) {
			renderTexture = new EditorTexture();
		}
	}

	public ETextures(Editor e, EditorTexture provider) {
		this.e = e;
		this.provider = provider;
	}

	public void free() {
		provider.free();
		if(renderTexture != null)renderTexture.free();
	}

	public void clean() {
		this.provider.texture = null;
		this.provider.setEdited(false);
		this.file = null;
		animatedTexs.clear();
	}

	public void markDirty() {
		provider.markDirty();
		refreshTexture();
	}

	public boolean isEdited() {
		return provider.isEdited();
	}

	public void setEdited(boolean edited) {
		provider.setEdited(edited);
	}

	public void refreshTexture(EditorTexture tex) {
		if(textureLoader == null) {
			if(renderTexture != null) {
				renderTexture.setImage(new Image(provider.getImage()));
				renderTexture.size = provider.size;
			}
			return;
		}
		if(stitcher.refresh(tex.getImage()))
			renderTexture.markDirty();
		updateAnim();
	}

	public void restitchTexture() {
		if(textureLoader == null) {
			if(renderTexture != null) {
				renderTexture.setImage(new Image(provider.getImage()));
				renderTexture.size = provider.size;
			}
			return;
		}
		stitcher = new TextureStitcher(MAX_TEX_SIZE);
		stitcher.setBase(provider);
		textureLoader.accept(stitcher);
		stitcher.finish(renderTexture);
		renderTexture.markDirty();
		updateAnim();
	}

	public void setRGB(int x, int y, int rgb) {
		provider.setRGB(x, y, rgb);
	}

	public void write(IOHelper dout) throws IOException {
		provider.write(dout);
	}

	public void setImage(Image image) {
		provider.setImage(image);
	}

	public void refreshTexture() {
		refreshTexture(provider);
	}

	public Image getImage() {
		return provider.getImage();
	}

	public boolean hasStitches() {
		if(textureLoader == null)return false;
		return stitcher.hasStitches();
	}

	public EditorTexture getRenderTexture() {
		return renderTexture != null ? renderTexture : provider;
	}

	@Override
	public String getName() {
		return e.gui().i18nFormat("label.cpm.texture." + type.name().toLowerCase());
	}

	@Override
	public ETextures getTexture() {
		return this;
	}

	public void setDefaultImg(Image defaultImg) {
		this.defaultImg = defaultImg;
	}

	public Image copyDefaultImg() {
		return new Image(defaultImg);
	}

	public boolean isEditable() {
		return type != null ? type.editable : false;
	}

	@Override
	public int textColor() {
		return isEdited() ? 0 : e.gui().getColors().button_text_disabled;
	}

	public void updateAnim() {
		if(!animatedTexs.isEmpty() && renderTexture != null) {
			animatedTexs.forEach(t -> {
				if(t.apply(renderTexture.getImage(), provider.getImage())) {
					renderTexture.markDirty();
				}
			});
		}
	}

	@Override
	public void updateGui() {
		e.setEnAddAnimTex.accept(type.editable);
	}

	@Override
	public void getTreeElements(Consumer<TreeElement> c) {
		if(type.editable)c.accept(animsList);
	}

	private class AnimTreeList implements TreeElement {

		@Override
		public String getName() {
			return e.gui().i18nFormat("label.cpm.tree.animatedTex");
		}

		@Override
		public void getTreeElements(Consumer<TreeElement> c) {
			animatedTexs.forEach(c);
		}

		@Override
		public ETextures getTexture() {
			return ETextures.this;
		}
	}

	public TextureSheetType getType() {
		return type;
	}

	public int getMaxSize() {
		return Math.max(provider.getImage().getWidth(), provider.getImage().getHeight());
	}
}
