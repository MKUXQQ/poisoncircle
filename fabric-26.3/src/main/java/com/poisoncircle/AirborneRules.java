package com.poisoncircle;

import java.util.*;

/** Pure bounded rules shared by the authoritative game and tests. */
final class AirborneRules {
    static final int COUNTDOWN_TICKS = 100;
    static final int BOARDING_LOCK_TICKS = 100;
    static final double MIN_SPEED = .2, MAX_SPEED = 2;
    private AirborneRules() {}
    static List<List<UUID>> groups(List<UUID> ids, String mode) {
        var out = new ArrayList<List<UUID>>();
        for (int i=0;i<ids.size();i += mode.equals("duo") ? 2 : 1) out.add(List.copyOf(ids.subList(i, Math.min(ids.size(), i + (mode.equals("duo") ? 2 : 1)))));
        return out;
    }
    static int cyclicPoint(int index, int size) { return size <= 0 ? -1 : Math.floorMod(index, size); }
    static double routeDistance(double horizontal, int extra) { return horizontal + Math.max(0, extra); }
    static Vec physics(float forward, float strafe, float yaw, float pitch, double speed) {
        double y = Math.toRadians(yaw), p = Math.toRadians(Math.max(-90, Math.min(90, pitch)));
        double thrust = .45 + Math.max(0, forward) * .55 + Math.min(0, forward) * .30;
        double h = Math.cos(p), x = (-Math.sin(y) * thrust + Math.cos(y) * strafe) * h * speed;
        double z = (Math.cos(y) * thrust + Math.sin(y) * strafe) * h * speed;
        return new Vec(x, -Math.max(.18, Math.abs(Math.sin(p)) * Math.max(.25, thrust) * speed), z);
    }
    /** Minecraft 26.3 fall-flying lift/drag equations, with bounded W/S braking and lateral steering. */
    static Vec glide(Vec previous, float forward, float strafe, float yaw, float pitch) {
        double p=Math.toRadians(Math.max(-90,Math.min(90,pitch))), r=Math.toRadians(yaw);
        double lx=-Math.sin(r)*Math.cos(p), lz=Math.cos(r)*Math.cos(p), look=Math.hypot(lx,lz);
        double horizontal=Math.hypot(previous.x,previous.z), lift=Math.cos(p)*Math.cos(p);
        double x=previous.x, y=previous.y+.08*(-1+lift*.75), z=previous.z;
        if(y<0&&look>1e-8){double exchange=y*-.1*lift;x+=lx*exchange/look;y+=exchange;z+=lz*exchange/look;}
        if(p<0&&look>1e-8){double climb=horizontal*-Math.sin(p)*.04;x-=lx*climb/look;y+=climb*3.2;z-=lz*climb/look;}
        if(look>1e-8){x+=(lx/look*horizontal-x)*.1;z+=(lz/look*horizontal-z)*.1;}
        double control=Math.max(-1,Math.min(1,forward)), brake=control<0?.96:1;
        x=(x+(-Math.sin(r)*Math.max(0,control)*.012+Math.cos(r)*strafe*.012))*brake*.99;
        z=(z+(Math.cos(r)*Math.max(0,control)*.012+Math.sin(r)*strafe*.012))*brake*.99;
        y*=.98;double magnitude=Math.sqrt(x*x+y*y+z*z);if(magnitude>3){double scale=3/magnitude;x*=scale;y*=scale;z*=scale;}
        return new Vec(x,y,z);
    }
    static boolean validConfig(double height, double speed) { return Double.isFinite(height) && Double.isFinite(speed) && height > 0 && speed >= MIN_SPEED && speed <= MAX_SPEED; }
    record Vec(double x, double y, double z) {}
}
