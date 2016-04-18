package com.mygdx.gravswarm;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
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
import com.badlogic.gdx.utils.Pool;

import java.util.Random;
import java.util.Vector;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class GravSwarm extends ApplicationAdapter {
	boolean speedCheck;

	Vector<Moon> moons;
	Pool<Gravity> freeGravities;
	Vector<Gravity>gravities;
	Vector<GravityHandler>gravityHandlers;
	Vector<Gravity>gravitiesToBeCulled;

	PerspectiveCamera cam;
	Model modelTemplate;
	ModelBatch modelBatch;
	Environment environment;
	CameraInputController camController;
	CoreInputProcessor coreInput;
	CyclicBarrier barrier;

	float LIGHT_INTENSITIY;
	float GRAVITY_PLANE_DISTANCE;
	int MOONS_TO_SPAWN;
	int THREAD_COUNT;
	
	@Override
	public void create () {
		speedCheck=false;
		Random rnd=new Random();
		moons=new Vector<Moon>();
		gravities=new Vector<Gravity>();
		gravitiesToBeCulled=new Vector<Gravity>();
		gravityHandlers=new Vector<GravityHandler>();
		freeGravities=new Pool<Gravity>() {
			@Override
			protected Gravity newObject() {
				return new Gravity(1f);
			}
		};

		LIGHT_INTENSITIY=50000f;
		GRAVITY_PLANE_DISTANCE=.7f;
		MOONS_TO_SPAWN=1000;
		THREAD_COUNT=3;

		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));

		coreInput=new CoreInputProcessor();
		Gdx.input.setInputProcessor(coreInput);

		modelBatch = new ModelBatch();
		barrier= new CyclicBarrier(THREAD_COUNT+1);

		cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.position.set(500f, 500f, 500f);
		cam.lookAt(0, 0, 0);
		cam.near = 1f;
		cam.far = 3000f;
		cam.update();


		ModelBuilder modelBuilder = new ModelBuilder();
		for(int x=0;x<MOONS_TO_SPAWN;++x)
		{
			modelTemplate=modelBuilder.createSphere(5f, 5f, 5f, 8, 5, new Material(ColorAttribute.createDiffuse(rnd.nextFloat(),rnd.nextFloat(),rnd.nextFloat(),0)), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
			moons.add(new Moon(modelTemplate));
			moons.elementAt(x).transform.translate((rnd.nextFloat() * 200) - 100, (rnd.nextFloat() * 200) - 100, (rnd.nextFloat() * 200) - 100);
		}
		int curr=0;
		int step=MOONS_TO_SPAWN/THREAD_COUNT;
		int next=step;
		for(int x=0;x<THREAD_COUNT;++x)
		{
			if(x<THREAD_COUNT-1)
			{
				gravityHandlers.add(new GravityHandler(curr, next));
				curr=next;
				next+=step+1;
			}
			else
				gravityHandlers.add(new GravityHandler(curr,-1));
			gravityHandlers.elementAt(x).start();
		}


	}

	@Override
	public void render() {
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		if(speedCheck)
		{
			for(int i=0;i<moons.size();++i)
				moons.elementAt(i).scaleVelocity(.7f);
		}
		try {
			barrier.await();//sync with the gravity threads before rendering
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			e.printStackTrace();
		}
		//singleCoreGravity();
		for(int moonNumber=0;moonNumber<moons.size();++moonNumber)
			moons.elementAt(moonNumber).move();
		modelBatch.begin(cam);
		modelBatch.render(moons, environment);
		modelBatch.end();
		while(gravitiesToBeCulled.size()>0)
		{
			freeGravities.free(gravitiesToBeCulled.elementAt(0));
			gravities.remove(gravitiesToBeCulled.elementAt(0));
			gravitiesToBeCulled.remove(0);
		}
		try {
			barrier.await();//restart gravity calculations
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			e.printStackTrace();
		}
	}

	void singleCoreGravity()
	{
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

	class CoreInputProcessor extends InputAdapter
	{
		Gravity[] touchGravities;
		Vector3 worldPoint;

		CoreInputProcessor()
		{
			touchGravities=new Gravity[10];
			worldPoint=new Vector3();
		}
		@Override public boolean touchDown(int x, int y, int pointer, int button)
		{
			if(button== Input.Buttons.LEFT)
			{
				worldPoint=cam.unproject(new Vector3(x, y, 1f));
				worldPoint.lerp(cam.unproject(new Vector3(x, y, 0f)),GRAVITY_PLANE_DISTANCE);
				touchGravities[pointer] = freeGravities.obtain();
				touchGravities[pointer].spawn(worldPoint);
				gravities.add(touchGravities[pointer]);
				return true;
			}
			return false;
		}
		@Override public boolean touchDragged(int x,int y,int pointer)
		{
			if(touchGravities[pointer]!=null)
			{
				worldPoint=cam.unproject(new Vector3(x, y, 1f));
				worldPoint.lerp(cam.unproject(new Vector3(x, y, 0f)),GRAVITY_PLANE_DISTANCE);
				touchGravities[pointer].setPosition(worldPoint);
				return true;
			}
			return false;
		}
		@Override public boolean touchUp(int x, int y, int pointer, int button)
		{
			if(button!= Input.Buttons.LEFT) return false;
			gravitiesToBeCulled.add(touchGravities[pointer]);
			touchGravities[pointer]=null;
			return true;
		}
		@Override public boolean keyDown(int keycode)
		{
			if(keycode==Input.Keys.SPACE)
			{
				speedCheck=true;
				return true;
			}
			return false;
		}
		@Override public boolean keyUp(int keycode)
		{
			if(keycode==Input.Keys.SPACE)
			{
				speedCheck=false;
				return true;
			}
			return false;
		}
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
		void scaleVelocity(float scalar){velocity.scl(scalar);}
	}

	class Gravity implements Pool.Poolable
	{
		Vector3 position;
		float magnitude;
		boolean quadratic;
		PointLight light;

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
			this(new Vector3(0f, 0f, 0f), magnitude);
		}

		Gravity(Vector3 position,float magnitude, boolean quadratic)
		{
			this.position=new Vector3(position);
			this.magnitude=magnitude;
			this.quadratic=quadratic;
			light=new PointLight();
			light.set(1f, 1f, 1f, position, LIGHT_INTENSITIY);
			//environment.add(light);
		}

		public void setQuadratic(boolean mode)
		{
			quadratic=mode;
		}

		public void setMagnitude(float magnitude) {
			this.magnitude = magnitude;
		}

		public void setPosition(Vector3 position) {
			this.position = position;
			light.setPosition(position);
		}

		@Override public void reset()
		{
			environment.remove(light);
		}

		public void spawn(Vector3 position)
		{
			environment.add(light);
			this.setPosition(position);
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
		int lowBound, highBound;

		GravityHandler()
		{
			this(-1,-1);
		}

		GravityHandler(int lowBound,int highBound)
		{
			this.lowBound=lowBound;
			this.highBound=highBound;
		}

		public void setBounds(int high, int low)
		{
			lowBound=low;
			highBound=high;
		}

		@Override public void run()
		{
			int gravityNumber,moonNumber;
			Vector3 position=new Vector3();
			while(!Thread.interrupted())
			{
				if(highBound==-1)
					highBound=moons.size();
				if(lowBound==-1)
					lowBound=0;
				moonNumber=lowBound;
				while(moonNumber<highBound)
				{
					for(gravityNumber=0;gravityNumber<gravities.size();++gravityNumber)
					{
						moons.elementAt(moonNumber).transform.getTranslation(position);
						moons.elementAt(moonNumber).updateVelocity(gravities.elementAt(gravityNumber).getAccel(position));
					}
					++moonNumber;
				}
				try {
					barrier.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (BrokenBarrierException e) {
					e.printStackTrace();
				}
				try {
					barrier.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (BrokenBarrierException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
