package cn.zbx1425.mtrsteamloco.render.rail;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MatrixCalculator {

    private static final double EPSILON = 1e-2;

    public static double[][] computeMatrix(double pz, List<double[]> transformedPoints) {
        // 生成归一化后的原始点
        List<double[]> originalPoints = generateOriginalPoints(pz);
        double halfPz = pz / 2;
        double scaleZ = 1.0 / halfPz;
        List<double[]> normalizedOriginalPoints = new ArrayList<>();
        for (double[] p : originalPoints) {
            normalizedOriginalPoints.add(new double[]{p[0], p[1], p[2] * scaleZ});
        }

        // 归一化变换后的点
        double[] centroid = computeCentroid(transformedPoints);
        List<double[]> translatedPoints = translatePoints(transformedPoints, centroid);
        double[] scales = computeScales(translatedPoints);
        List<double[]> normalizedTransformedPoints = scalePoints(translatedPoints, scales);

        // 计算归一化后的变换矩阵
        double[][] MPrime = computeMPrime(normalizedOriginalPoints, normalizedTransformedPoints);

        // 构建调整矩阵
        RealMatrix SOriginal = createScalingMatrix(1, 1, scaleZ);
        RealMatrix STransformedInv = createScalingMatrix(1/scales[0], 1/scales[1], 1/scales[2]);
        RealMatrix T = createTranslationMatrix(centroid[0], centroid[1], centroid[2]);
        RealMatrix MPrimeMat = MatrixUtils.createRealMatrix(MPrime);

        // 计算最终矩阵: T * STransformedInv * MPrime * SOriginal
        RealMatrix M = T.multiply(STransformedInv.multiply(MPrimeMat.multiply(SOriginal)));

        // 转换为二维数组并归一化
        double[][] matrix = matrixToArray(M);
        normalizeMatrix(matrix);

        System.out.println(verifyMatrix(matrix, transformedPoints, pz));
        return matrix;
    }

    private static double[][] computeMPrime(List<double[]> originalPoints, List<double[]> transformedPoints) {
        double[][] A = new double[24 * 8][16];
        int row = 0;
        for (int i = 0; i < 8; i++) {
            double[] original = originalPoints.get(i);
            double x = original[0], y = original[1], z = original[2];
            double[] transformed = transformedPoints.get(i);
            double xp = transformed[0], yp = transformed[1], zp = transformed[2];

            fillRow(A[row], x, y, z, xp, 0, 4, 8, 12, 3, 7, 11, 15);
            row++;
            fillRow(A[row], x, y, z, yp, 1, 5, 9, 13, 3, 7, 11, 15);
            row++;
            fillRow(A[row], x, y, z, zp, 2, 6, 10, 14, 3, 7, 11, 15);
            row++;
        }

        RealMatrix matrixA = MatrixUtils.createRealMatrix(Arrays.copyOf(A, row));
        SingularValueDecomposition svd = new SingularValueDecomposition(matrixA);
        RealMatrix V = svd.getV();
        double[] m = V.getColumn(V.getColumnDimension() - 1);

        double[][] matrix = new double[4][4];
        for (int i = 0; i < 16; i++) {
            int col = i / 4;
            int r = i % 4;
            matrix[r][col] = m[i];
        }
        return matrix;
    }

    private static void fillRow(double[] row, double x, double y, double z, double prime,
                                int m0, int m4, int m8, int m12, 
                                int m3, int m7, int m11, int m15) {
        Arrays.fill(row, 0.0);
        row[m0] = x;
        row[m4] = y;
        row[m8] = z;
        row[m12] = 1.0;

        row[m3] = -prime * x;
        row[m7] = -prime * y;
        row[m11] = -prime * z;
        row[m15] = -prime;
    }

    private static List<double[]> generateOriginalPoints(double pz) {
        double halfPz = pz / 2;
        return Arrays.asList(
            new double[]{-1, 1, -halfPz}, new double[]{1, 1, -halfPz},
            new double[]{1, -1, -halfPz}, new double[]{-1, -1, -halfPz},
            new double[]{-1, 1, halfPz}, new double[]{1, 1, halfPz},
            new double[]{1, -1, halfPz}, new double[]{-1, -1, halfPz}
        );
    }

    private static double[] computeCentroid(List<double[]> points) {
        double txSum = 0, tySum = 0, tzSum = 0;
        for (double[] p : points) {
            txSum += p[0];
            tySum += p[1];
            tzSum += p[2];
        }
        return new double[]{txSum / 8, tySum / 8, tzSum / 8};
    }

    private static List<double[]> translatePoints(List<double[]> points, double[] centroid) {
        List<double[]> translated = new ArrayList<>();
        for (double[] p : points) {
            translated.add(new double[]{
                p[0] - centroid[0],
                p[1] - centroid[1],
                p[2] - centroid[2]
            });
        }
        return translated;
    }

    private static double[] computeScales(List<double[]> points) {
        double maxX = 0, maxY = 0, maxZ = 0;
        for (double[] p : points) {
            maxX = Math.max(maxX, Math.abs(p[0]));
            maxY = Math.max(maxY, Math.abs(p[1]));
            maxZ = Math.max(maxZ, Math.abs(p[2]));
        }
        return new double[]{
            maxX > 1e-10 ? 1.0 / maxX : 1.0,
            maxY > 1e-10 ? 1.0 / maxY : 1.0,
            maxZ > 1e-10 ? 1.0 / maxZ : 1.0
        };
    }

    private static List<double[]> scalePoints(List<double[]> points, double[] scales) {
        List<double[]> scaled = new ArrayList<>();
        for (double[] p : points) {
            scaled.add(new double[]{
                p[0] * scales[0],
                p[1] * scales[1],
                p[2] * scales[2]
            });
        }
        return scaled;
    }

    private static RealMatrix createScalingMatrix(double sx, double sy, double sz) {
        return MatrixUtils.createRealMatrix(new double[][]{
            {sx, 0, 0, 0},
            {0, sy, 0, 0},
            {0, 0, sz, 0},
            {0, 0, 0, 1}
        });
    }

    private static RealMatrix createTranslationMatrix(double tx, double ty, double tz) {
        return MatrixUtils.createRealMatrix(new double[][]{
            {1, 0, 0, tx},
            {0, 1, 0, ty},
            {0, 0, 1, tz},
            {0, 0, 0, 1}
        });
    }

    private static double[][] matrixToArray(RealMatrix matrix) {
        double[][] array = new double[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                array[i][j] = matrix.getEntry(i, j);
            }
        }
        return array;
    }

    private static void normalizeMatrix(double[][] matrix) {
        double maxAbs = 0.0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                maxAbs = Math.max(maxAbs, Math.abs(matrix[i][j]));
            }
        }
        if (maxAbs > 1e-10) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    matrix[i][j] /= maxAbs;
                }
            }
        }
    }

    public static boolean verifyMatrix(double[][] matrix, List<double[]> transformedPoints, double pz) {
        List<double[]> originalPoints = generateOriginalPoints(pz);
        for (int i = 0; i < originalPoints.size(); i++) {
            double[] original = originalPoints.get(i);
            double[] transformed = transformedPoints.get(i);
            
            double x = original[0], y = original[1], z = original[2];
            double tx = matrix[0][0] * x + matrix[0][1] * y + matrix[0][2] * z + matrix[0][3];
            double ty = matrix[1][0] * x + matrix[1][1] * y + matrix[1][2] * z + matrix[1][3];
            double tz = matrix[2][0] * x + matrix[2][1] * y + matrix[2][2] * z + matrix[2][3];
            double w = matrix[3][0] * x + matrix[3][1] * y + matrix[3][2] * z + matrix[3][3];

            if (Math.abs(w - 1.0) > EPSILON) {
                tx /= w;
                ty /= w;
                tz /= w;
            }

            if (Math.abs(tx - transformed[0]) > EPSILON ||
                Math.abs(ty - transformed[1]) > EPSILON || 
                Math.abs(tz - transformed[2]) > EPSILON) {
                return false;
            }
        }
        return true;
    }
}