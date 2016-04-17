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
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

import java.util.Random;
import java.util.Vector;

public class GravSwarm extends ApplicationAdapter {
	PerspectiveCamera cam;
	Vector<Moon> moons;
	Vector<Gravity>gravities;
	Vector<GravityHandler>gravityHandlers;
	Model modelTemplate;
	ModelBatch modelBatch;
	Environment environment;
	CameraInputController camController;
	
	@Override
	public void create () {
		Random rnd=new Random();
		moons=new Vector<Moon>();
		gravities=new Vector<Gravity>();
		gravityHandlers=new Vector<GravityHandler>();

		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new PointLight().set(1f, 1f, 1f, 0f, 0f, 0f, 10000f));

		modelBatch = new ModelBatch();

		cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.position.set(500f, 500f, 500f);
		cam.lookAt(0, 0, 0);
		cam.near = 1f;
		cam.far = 3000f;
		cam.update();


		ModelBuilder modelBuilder = new ModelBuilder();
		modelTemplate=modelBuilder.createSphere(5f, 5f, 5f, 8, 5, new Material(ColorAttribute.createDiffuse(rnd.nextFloat(),rnd.nextFloat(),rnd.nextFloat(),0)), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		for(int x=0;x<500;++x)
		{
			modelTemplate=modelBuilder.createSphere(5f, 5f, 5f, 8, 5, new Material(ColorAttribute.createDiffuse(rnd.nextFloat(),rnd.nextFloat(),rnd.nextFloat(),0)), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
			moons.add(new Moon(modelTemplate));
			moons.elementAt(x).transform.translate((rnd.nextFloat() * 100)-50, (rnd.nextFloat() * 100)-50, (rnd.nextFloat() * 100)-50);
			moons.elementAt(x).updateVelocity(new Vector3((rnd.nextFloat()*20)-10,(rnd.nextFloat()*20)-10,(rnd.nextFloat()*20)-10));

		}

		gravities.add(new Gravity(.1f));
//		gravityHandlers.add(new GravityHandler(gravities, moons));
//		gravityHandlers.elementAt(0).start();

		camController = new CameraInputController(cam);
		Gdx.input.setInputProcessor(camController);
	}

	@Override
	public void render() {
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

//		for(int x=0;x<gravityHandlers.size();++x)
//		{
//			gravityHandlers.elementAt(x).notify();
//		}
		singleCoreGravity();

		modelBatch.begin(cam);
		modelBatch.render(moons, environment);
		modelBatch.end();
//		try {
//			gravityHandlers.elementAt(0).wait();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
	}

	void singleCoreGravity()
	{
		Vector3 testVec=new Vector3(-.01f,-.01f,-.01f);
		Vector3 position=new Vector3();
		int x=0;
		int y;
		while(x<moons.size())
		{
			for(y=0;y<gravities.size();++y)
			{
				moons.elementAt(x).transform.getTranslation(position);
				moons.elementAt(x).updateVelocity(gravities.elementAt(y).getAccel(position));
			}
			moons.elementAt(x).move();
			++x;
		}
	}

	@Override
	public void dispose () {
		//model.dispose();
	}

	class Moon extends ModelInstance
	{
		Vector3 velocity;
		Moon(Model template)
		{
			super(template);
			velocity=new Vector3();
		}
		void updateVelocity(Vector3 change)
		{
			velocity.add(change);
		}
		void move()
		{
			this.transform.translate(velocity);
		}
	}

	class Gravity
	{
		Vector3 position;
		float magnitude;
		boolean quadratic;

		Gravity()
		{
			this(new Vector3(0f,0f,0f),1,false);
		}

		Gravity(Vector3 position)
		{
			this(position,1,false);
		}

		Gravity(Vector3 position,float magnitude)
		{
			this(position, magnitude,false);
		}
		Gravity(float magnitude)
		{
			this(new Vector3(0f,0f,0f),magnitude);
		}

		Gravity(Vector3 position,float magnitude, boolean quadratic)
		{
			this.position=new Vector3(position);
			this.magnitude=magnitude;
			this.quadratic=quadratic;
		}

		public void setQuadratic(boolean mode)
		{
			quadratic=mode;
		}

		public void setMagnitude(float magnitude) {
			this.magnitude = magnitude;
		}

		public Vector3 getAccel(Vector3 satPos)
		{
			Vector3 output;
			output=satPos.sub(position);
			output.nor();
			if(quadratic)
			{
				output.scl(-magnitude*(1/position.dst2(satPos)));
			}
			else
			{
				output.scl(-magnitude);
			}
			return output;
		}
	}

	class GravityHandler extends Thread
	{
		Vector<Gravity> gravities;
		Vector<Moon> moons;
		int lowBound, highBound;

		GravityHandler(Vector<Gravity> gravities,Vector<Moon> moons)
		{
			this(gravities, moons,-1,-1);
		}

		GravityHandler(Vector<Gravity> gravities,Vector<Moon> moons,int lowBound,int highBound)
		{
			this.gravities=gravities;
			this.moons=moons;
			this.lowBound=lowBound;
			this.highBound=highBound;
		}

		@Override public void run()
		{
			int y,x;
			Vector3 position=new Vector3();
			while(!Thread.interrupted())
			{
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if(highBound==-1)
					highBound=moons.size();
				if(lowBound==-1)
					lowBound=0;
				x=lowBound;
				while(x<highBound)
				{
					for(y=0;y<gravities.size();++y)
					{
						moons.elementAt(x).transform.getTranslation(position);
						moons.elementAt(x).updateVelocity(gravities.elementAt(y).getAccel(position));
					}
					moons.elementAt(x).move();
					++x;
				}
				notify();

			}
		}
	}
}
