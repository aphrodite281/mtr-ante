package cn.zbx1425.mtrsteamloco.data;

import net.minecraft.core.BlockPos;
import mtr.data.Rail;
import mtr.data.RailType;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public interface RailExtraSupplier {

    // "": default, "null": hidden
    String getModelKey();

    void setModelKey(String key);

    boolean getRenderReversed();

    void setRenderReversed(boolean value);

    float getVerticalCurveRadius();

    void setVerticalCurveRadius(float value);

    int getHeight();

    Map<Double, Float> getRollAngleMap();

    void setRollAngleMap(Map<Double, Float> rollAngleMap);

    Map<String, String> getCustomConfigs();
    
    void setCustomConfigs(Map<String, String> customConfigs);

    Map<String, ConfigResponder> getCustomResponders();

    void setCustomResponders(Map<String, ConfigResponder> customResponders);

    void setOpeningDirection(int direction);

    void setOpeningDirectionRaw(int direction);

    int getOpeningDirection();

    int getOpeningDirectionRaw();

    void setRailType(RailType railType);

    void partialCopyFrom(Rail rail);
    
    void setRollingOffset(float rollingOffset);

    float getRollingOffset();

    boolean isStraightOnly();

    void changePathMode(int mode);

    int getPathMode();

    boolean isBetween(double x, double y, double z, double radius);

    Rail getTransposition(RailType railType);

    void setBezier(BezierCurve bezier);

    boolean couldSwitchModeTo(int mode);

    void sendUpdateC2S();

    BlockPos getPosStart();

    BlockPos getPosEnd();

    static float getVTheta(Rail rail, double verticalCurveRadius) {
        double H = Math.abs(((RailExtraSupplier) rail).getHeight());
        double L = rail.getLength();
        double R = verticalCurveRadius;
        return 2 * (float) Mth.atan2(Math.sqrt(H * H - 4 * R * H + L * L) - L, H - 4 * R);
    }

    static float getRollAngle(Rail rail, double value) {
        Map<Double, Float> rollAngleMap = ((RailExtraSupplier) rail).getRollAngleMap();
        boolean reversed = ((RailExtraSupplier) rail).getRenderReversed();
        float k = reversed ? -1F : 1F;
        
        if (reversed) {
            value = rail.getLength() - value;
        }
        
        if (rollAngleMap.isEmpty()) {
            return 0;
        }
        
        List<Double> keys = new ArrayList<>(rollAngleMap.keySet());
        keys.sort(Double::compareTo);
        
        if (value <= keys.get(0)) {
            return k * rollAngleMap.get(keys.get(0));
        }
        
        int size = keys.size();
        if (value >= keys.get(size - 1)) {
            return k * rollAngleMap.get(keys.get(size - 1));
        }
        
        double lastKey = keys.get(0);
        for (int i = 1; i < size; i++) {
            double currentKey = keys.get(i);
            if (lastKey <= value && value < currentKey) {
                double t = (value - lastKey) / (currentKey - lastKey);
                double cosFactor = (1 - Math.cos(t * Math.PI)) / 2.0;
                float interpolated = (float) (rollAngleMap.get(lastKey) * (1 - cosFactor) 
                                            + rollAngleMap.get(currentKey) * cosFactor);
                return k * interpolated;
            }
            lastKey = currentKey;
        }
        return 0;
    }
}
