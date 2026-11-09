package com.poisoncircle;

/** Server-owned seated boarding, rear cargo ramp and passenger departure timeline. */
final class AirborneDeparture {
    static final int DOOR_TICKS=30, STAND_TICKS=12, AISLE_TICKS=8, WALK_TICKS=16, RAMP_TICKS=12, EXIT_TICKS=6;
    static final int RELEASE_TICKS=DOOR_TICKS+STAND_TICKS+AISLE_TICKS+WALK_TICKS+RAMP_TICKS+EXIT_TICKS;
    static final double FLOOR_Y=-2, SEAT_FEET_Y=-1.736875, RAMP_END_Y=-2.555674169, RAMP_END_Z=-12.151384810;
    record Position(double x,double y,double z) {}
    private AirborneDeparture() {}
    static double doorProgress(int elapsed){return Math.max(0,Math.min(1,elapsed/(double)DOOR_TICKS));}
    static Position seat(int seat){return new Position(seat==0?-1.9:1.9,SEAT_FEET_Y,-4);}
    static Position position(int elapsed,int seat){
        Position sitting=seat(seat);
        int t=Math.max(0,elapsed-DOOR_TICKS);
        if(t<=STAND_TICKS)return between(sitting,new Position(sitting.x,FLOOR_Y,-5.2),t/(double)STAND_TICKS);
        t-=STAND_TICKS;
        double lane=seat==0?-.55:.55;
        if(t<=AISLE_TICKS)return between(new Position(sitting.x,FLOOR_Y,-5.2),new Position(lane,FLOOR_Y,-5.2),t/(double)AISLE_TICKS);
        t-=AISLE_TICKS;
        if(t<=WALK_TICKS)return between(new Position(lane,FLOOR_Y,-5.2),new Position(lane,FLOOR_Y,-8.5),t/(double)WALK_TICKS);
        t-=WALK_TICKS;
        if(t<=RAMP_TICKS)return between(new Position(lane,FLOOR_Y,-8.5),new Position(lane,RAMP_END_Y,RAMP_END_Z),t/(double)RAMP_TICKS);
        return between(new Position(lane,RAMP_END_Y,RAMP_END_Z),new Position(lane,-2.85,-13),(t-RAMP_TICKS)/(double)EXIT_TICKS);
    }
    static boolean release(int elapsed){return elapsed>=RELEASE_TICKS;}
    static double approachStep(double remaining,double speed,boolean cruiseReady){return cruiseReady?Math.min(Math.max(0,remaining),speed):0;}
    private static Position between(Position a,Position b,double t){t=Math.max(0,Math.min(1,t));return new Position(a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t,a.z+(b.z-a.z)*t);}
}
