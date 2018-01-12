package com.example.vyaas.breakout;

import android.graphics.RectF;

import java.util.Random;

public class Ball {
    RectF rect;
    float XSpeed;
    float ySpeed;
    float width = 10;
    float height = 10;



    public Ball(){

        // Start the ball travelling straight up at 100 pixels per second
        XSpeed = 200;
        ySpeed = -400;

        // Place the ball in the centre of the screen at the bottom
        // Make it a 10 pixel x 10 pixel square
        rect = new RectF();

    }

    public RectF getRect(){
        return rect;
    }

    public void update(long fps){
        rect.left = rect.left + (XSpeed / fps);
        rect.top = rect.top + (ySpeed / fps);
        rect.right = rect.left + width;
        rect.bottom = rect.top - height;
    }

    public void reverseYVelocity(){
        ySpeed = -ySpeed;
    }

    public void reverseXVelocity(){
        XSpeed = - XSpeed;
    }

    public void setRandomXVelocity(){
        Random generator = new Random();
        int answer = generator.nextInt(2);

        if(answer == 0){
            reverseXVelocity();
        }
    }

    public void clearObstacleY(float y){
        rect.bottom = y;
        rect.top = y - height;
    }

    public void clearObstacleX(float x){
        rect.left = x;
        rect.right = x + width;
    }

    public void reset(int x, int y){
        rect.left = x / 2;
        rect.top = y - 20;
        rect.right = x / 2 + width;
        rect.bottom = y - 20 - height;
    }

}