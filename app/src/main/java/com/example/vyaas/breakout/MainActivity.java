package com.example.vyaas.breakout;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {

    // gameView will be the view of the game
    // It will also hold the logic of the game
    // and respond to screen touches as well
    GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //an easy way to get permissions for read write is to use dexter
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).withListener(new MultiplePermissionsListener() {
            @Override public void onPermissionsChecked(MultiplePermissionsReport report) {/* ... */}
            @Override public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {/* ... */}
        }).check();

        // Initialize gameView and set it as the view
        gameView = new GameView(this);
        setContentView(gameView);

    }

    // Here is our implementation of GameView
    
    class GameView extends SurfaceView implements Runnable,SensorEventListener {

        
        Thread gameThread = null;
        //a surfaceholder to hold the screen content
        
        SurfaceHolder holder;

        // A boolean which we will set and unset
        // when the game is running- or not.
        volatile boolean playing;

        // Game is paused at the start
        boolean paused = true;

        // A Canvas and a Paint object
        Canvas canvas;
        Paint paint;

        // This variable tracks the game frame rate
        long fps=60;
        
        // The size of the screen in pixels
        int X;
        int Y;

        // The players paddle
        Paddle paddle;

        // A ball
        Ball ball;

        //A rectangle for the pause button
        RectF pauseBrick ;
        // Up to 200 bricks
        Brick[] bricks = new Brick[200];
        int numBricks = 0;



        // The score
        int score = 0;
        // The high score
        int high_score = 0;
        // Lives
        int lives = 3;
        //speed
        int speed=0;
        //a file for highscore
        File file;
        // variables for shake detection
        private static final float SHAKE_THRESHOLD = 3.25f; // m/S**2
        private static final int MIN_TIME_BETWEEN_SHAKES_MILLISECS = 1000;
        private long mLastShakeTime;
        private SensorManager mSensorMgr= (SensorManager) getSystemService(SENSOR_SERVICE);;


        // Listen for shakes
        Sensor accelerometer = mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


        // When the we initialize (call new()) on gameView
        // This special constructor method runs
        public GameView(Context context) {
            // The next line of code asks the
            // SurfaceView class to set up our object.
            // How kind.
            super(context);

            // Initialize holder and paint objects
            holder = getHolder();
            paint = new Paint();

            // Get a Display object to access screen details
            Display display = getWindowManager().getDefaultDisplay();
            // Load the resolution into a Point object
            Point size = new Point();
            display.getSize(size);

            X = size.x;
            Y = size.y;

            paddle = new Paddle(X, Y);

            // Create a ball
            ball = new Ball();

            if (accelerometer != null) {
                mSensorMgr.registerListener((SensorEventListener) this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }


            file = new File(Environment.getExternalStorageDirectory(),"HighScore.txt");
            if(!file.exists())
            {
                try {
                    file.createNewFile();
                    high_score=0;
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
            else{
                readFile(file);
            }

            initialize();

        }

        public void initialize() {

            // Put the ball back to the start
            ball.reset(X, Y);

            int brickWidth = X / 20;
            int brickHeight = Y / 25;

            // Build a wall of bricks
            numBricks = 0;
            for (int column = 0; column < 20; column++) {
                for (int row = 1; row < 9; row++) {
                    bricks[numBricks] = new Brick(row, column, brickWidth, brickHeight);
                    numBricks++;
                }
            }
            // if game over reset scores and lives
            if (lives == 0) {
                if(high_score>score)
                {

                }else
                {
                    high_score=score;
                    save(file);
                }
                score = 0;
                lives = 3;
                speed=0;
                fps=60;
            }
        }

        @Override
        public void run() {
            while (playing) {
                // Capture the current time in milliseconds in startFrameTime
                long startFrameTime = System.currentTimeMillis();
                // Update the frame
                if (!paused) {
                    update();
                }
                // Draw the frame
                draw();            }

        }

        // Everything that needs to be updated goes in here
        // Movement, collision detection etc.
        public void update() {

            // Move the paddle if required
            paddle.update(fps);

            ball.update(fps-10);

            // Check for ball colliding with a brick
            for (int i = 0; i < numBricks; i++) {
                if (bricks[i].getVisibility()) {
                    if (RectF.intersects(bricks[i].getRect(), ball.getRect())) {
                        bricks[i].setInvisible();
                        ball.reverseYVelocity();
                        score = score + 10;

                    }
                }
            }
            //if the visibilitycount is less than a particular value, we get another row of bricks
            int visibilityCounter=0;
            for (int i = 0; i < numBricks; i++) {
                if (bricks[i].getVisibility()) {
                   visibilityCounter++;
                }
            }
            if(visibilityCounter<=10)
            {
                for(int i=1;i<=10;i++)
                {
                    Random r = new Random();
                    int x= r.nextInt(160);
                    bricks[x].setVisible();
                }
            }

            // Check for ball colliding with paddle
            if (RectF.intersects(paddle.getRect(), ball.getRect())) {
                ball.setRandomXVelocity();
                ball.reverseYVelocity();
                ball.clearObstacleY(paddle.getRect().top - 2);

            }
            // Bounce the ball back when it hits the bottom of screen
            if (ball.getRect().bottom > Y) {
                if (RectF.intersects(paddle.getRect(), ball.getRect())) {
                    ball.setRandomXVelocity();
                    ball.reverseYVelocity();
                    ball.clearObstacleY(paddle.getRect().top - 2);

                }
                else {
                    ball.reverseYVelocity();
                    ball.clearObstacleY(Y - 2);
                    ball.reset(X,Y);
                    // Lose a life
                    lives--;
                }

                if (lives == 0) {
                    paused = true;
                    initialize();
                }
            }

            // Bounce the ball back when it hits the top of screen
            if (ball.getRect().top < 0)

            {
                ball.reverseYVelocity();
                ball.clearObstacleY(12);


            }

            // If the ball hits left wall bounce
            if (ball.getRect().left < 0)

            {
                ball.reverseXVelocity();
                ball.clearObstacleX(2);

            }

            // If the ball hits right wall bounce
            if (ball.getRect().right > X - 10) {

                ball.reverseXVelocity();
                ball.clearObstacleX(X - 22);


            }


        }

        // Draw the newly updated scene
        public void draw() {

            // Make sure our drawing surface is valid or we crash
            if (holder.getSurface().isValid()) {
                // Lock the canvas ready to draw
                canvas = holder.lockCanvas();

                // Draw the background color
                canvas.drawColor(Color.argb(255, 0, 0, 0));

                // Choose the brush color for drawing
                paint.setColor(Color.argb(255, 255, 255, 255));

                // Draw the paddle
                canvas.drawRect(paddle.getRect(), paint);

                // Draw the ball
                canvas.drawRect(ball.getRect(), paint);


                // Change the brush color for drawing
                paint.setColor(Color.argb(255, 249, 129, 0));

                // Draw the bricks if visible
                for (int i = 0; i < numBricks; i++) {
                    if(i%8==0||i%8==1)
                    {
                        paint.setColor(Color.RED);
                    }
                    else if(i%8==2||i%8==3)
                    {
                        paint.setColor(Color.YELLOW);
                    }
                    else if(i%8==4||i%8==5)
                    {
                        paint.setColor(Color.GREEN);
                    }
                    else if(i%8==6||i%8==7)
                    {
                        paint.setColor(Color.BLUE);
                    }
                    if (bricks[i].getVisibility()) {
                        canvas.drawRect(bricks[i].getRect(), paint);
                    }
                }

                // Choose the brush color for drawing
                paint.setColor(Color.argb(255, 255, 255, 255));

                // Draw the score
                paint.setTextSize(40);
                canvas.drawText("Score: " + score + "   Lives: " + lives +" High Score: " + high_score +"   Speed: "+speed, 10, 50, paint);
                pauseBrick= new RectF(X-(X/10),0,X,Y/25);
                canvas.drawRect(pauseBrick,paint);
                paint.setColor(Color.BLACK);
                paint.setTextSize(45);
                canvas.drawText("PAUSE",X-(X/10)+1,Y/30,paint);


                // Draw everything to the screen
                holder.unlockCanvasAndPost(canvas);
            }
        }

        // If SimpleGameEngine Activity is paused/stopped
        // shutdown our thread.
        public void pause() {

            playing = false;
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                Log.e("Error:", "joining thread");
            }
        }

        // If SimpleGameEngine Activity is started then
        // start our thread.
        public void resume() {
            playing = true;
            gameThread = new Thread(this);
            gameThread.start();
        }

        // The SurfaceView class implements onTouchListener
        // So we can override this method and detect screen touches.
        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {
            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                // Player has touched the screen
                case MotionEvent.ACTION_DOWN:
                    paused = false;
                    if (motionEvent.getX() > X / 2) {

                        paddle.setMovementState(paddle.RIGHT);
                    } else
                    {
                        paddle.setMovementState(paddle.LEFT);
                    }
                    if(motionEvent.getX()>X-(X/5)&&motionEvent.getY()<(Y/20))
                    {
                        if(playing) {

                            pause();
                        }else{
                            resume();
                        }

                    }


                    break;

                // Player has removed finger from screen
                case MotionEvent.ACTION_UP:

                    paddle.setMovementState(paddle.STOPPED);
                    break;
            }

            return true;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                long curTime = System.currentTimeMillis();
                if ((curTime - mLastShakeTime) > MIN_TIME_BETWEEN_SHAKES_MILLISECS) {

                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];

                    double acceleration = Math.sqrt(Math.pow(x, 2) +
                            Math.pow(y, 2) +
                            Math.pow(z, 2)) - SensorManager.GRAVITY_EARTH;

                    if (acceleration > SHAKE_THRESHOLD) {
                        mLastShakeTime = curTime;
                        fps=fps-5;
                        speed++;

                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Ignore
        }

        public void save(File file)
        {
            FileOutputStream fos = null;

                try{

                    fos= new FileOutputStream(file);
                    fos.write(String.valueOf(high_score).getBytes());
                    fos.close();

                }
                catch(FileNotFoundException e){
                    e.printStackTrace();
                    return;
                }
                catch (IOException e){
                    e.printStackTrace();
                    return;
                }

            }
        //A function to read the text file
        public void readFile(File file)
        {
            FileInputStream fis = null;
            try{
                fis = new FileInputStream(file);
                InputStreamReader ios = new InputStreamReader(fis);
                BufferedReader bs = new BufferedReader(ios);

                while(bs!=null) {
                    String s = bs.readLine();
                    if(s==null){
                        break;
                    }
                    else
                        {
                            if(s.length()==0)
                            {
                              high_score=0;
                            }
                            else {
                                high_score = Integer.valueOf(s);
                            }
                    }
                }
            }
            catch(FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }



    }
    // This is the end of our GameView inner class

    // This method executes when the player starts the game
    @Override
    protected void onResume() {
        super.onResume();
        // Tell the gameView resume method to execute
        gameView.resume();
    }

    // This method executes when the player quits the game
    @Override
    protected void onPause() {
        super.onPause();

        // Tell the gameView pause method to execute
        gameView.pause();
    }

}
// This is the end of the BreakoutGame class