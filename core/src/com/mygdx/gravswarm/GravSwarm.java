package com.mygdx.gravswarm;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;

import java.util.Random;
import java.util.Vector;

public class GravSwarm extends ApplicationAdapter {
	PerspectiveCamera cam;
	Vector<ModelInstance> models;
	Model modelTemplate;
	ModelInstance instance;
	ModelBatch modelBatch;
	Environment environment;
	CameraInputController camController;
	
	@Override
	public void create () {
		Random rnd=new Random();
		models=new Vector<ModelInstance>();
		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

		modelBatch = new ModelBatch();

		cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.position.set(100f, 100f, 100f);
		cam.lookAt(0,0,0);
		cam.near = 1f;
		cam.far = 300f;
		cam.update();

		ModelBuilder modelBuilder = new ModelBuilder();
		modelTemplate=modelBuilder.createSphere(5f, 5f, 5f, 8, 5, new Material(ColorAttribute.createDiffuse(Color.GREEN)), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		for(int x=0;x<10;++x)
		{
			models.add(new ModelInstance(modelTemplate));
			models.elementAt(x).transform.translate(rnd.nextFloat()*100,rnd.nextFloat()*100,rnd.nextFloat()*100);
		}
		camController = new CameraInputController(cam);
		Gdx.input.setInputProcessor(camController);
	}

	@Override
	public void render() {
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		modelBatch.begin(cam);
		modelBatch.render(models,environment);
		modelBatch.end();
	}

	@Override
	public void dispose () {
		//model.dispose();
	}
}
