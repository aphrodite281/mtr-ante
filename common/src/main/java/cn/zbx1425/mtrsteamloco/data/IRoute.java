package cn.zbx1425.mtrsteamloco.data;

import mtr.path.PathData;

import java.util.List;

public interface IRoute {
    List<PathData> getPathData();
    void setPathData(List<PathData> pathData);
    // void setAllData(FriendlyByteBuf buffer);
}