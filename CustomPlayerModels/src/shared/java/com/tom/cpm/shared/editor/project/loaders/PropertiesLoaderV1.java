package com.tom.cpm.shared.editor.project.loaders;

import java.io.IOException;

import com.tom.cpl.math.Vec3f;
import com.tom.cpm.shared.editor.Editor;
import com.tom.cpm.shared.editor.project.IProject;
import com.tom.cpm.shared.editor.project.JsonMap;
import com.tom.cpm.shared.editor.project.ProjectPartLoader;
import com.tom.cpm.shared.editor.project.ProjectWriter;
import com.tom.cpm.shared.model.SkinType;
import com.tom.cpm.shared.util.ScalingOptions;

public class PropertiesLoaderV1 implements ProjectPartLoader {

	@Override
	public String getId() {
		return "prop";
	}

	@Override
	public int getVersion() {
		return 1;
	}

	@Override
	public void load(Editor editor, IProject project) throws IOException {
		JsonMap data = project.getJson("config.json");
		if(data.containsKey("skinType")) {
			editor.customSkinType = true;
			editor.skinType = SkinType.get(data.getString("skinType"));
		}
		editor.hideHeadIfSkull = data.getBoolean("hideHeadIfSkull", true);
		editor.removeArmorOffset = data.getBoolean("removeArmorOffset", !editor.elements.stream().anyMatch(e -> e.duplicated));
		float defScaling = data.getFloat("scaling", 0);
		editor.scalingElem.enabled = defScaling != 0;
		editor.scalingElem.scaling.put(ScalingOptions.ENTITY, defScaling);
		JsonMap scaling = data.getMap("scalingEx");
		if(scaling != null) {
			for(ScalingOptions opt : ScalingOptions.VALUES) {
				if(opt == ScalingOptions.ENTITY)continue;
				float v = scaling.getFloat(opt.name().toLowerCase(), 0);
				if(v != 0)
					editor.scalingElem.scaling.put(opt, v);
			}
			editor.scalingElem.pos = new Vec3f(scaling.getMap("render_position"), new Vec3f());
			editor.scalingElem.rotation = new Vec3f(scaling.getMap("render_rotation"), new Vec3f());
			editor.scalingElem.scale = new Vec3f(scaling.getMap("render_scale"), new Vec3f());
		}
	}

	@Override
	public void save(Editor editor, ProjectWriter project) throws IOException {
		JsonMap data = project.getJson("config.json");
		data.put("skinType", editor.skinType.getName());
		data.put("scaling", editor.scalingElem.enabled ? editor.scalingElem.getScale() : 0);
		data.put("hideHeadIfSkull", editor.hideHeadIfSkull);
		data.put("removeArmorOffset", editor.removeArmorOffset);
		JsonMap scaling = data.putMap("scalingEx");
		for(ScalingOptions opt : ScalingOptions.VALUES) {
			if(opt == ScalingOptions.ENTITY)continue;
			scaling.put(opt.name().toLowerCase(), editor.scalingElem.getScale(opt));
		}
		scaling.put("render_position", editor.scalingElem.pos.toMap());
		scaling.put("render_rotation", editor.scalingElem.rotation.toMap());
		scaling.put("render_scale", editor.scalingElem.scale.toMap());
	}

}
