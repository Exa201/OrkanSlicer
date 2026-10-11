/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.ytkab0bp.slicebeam.utils

import androidx.annotation.NonNull

/**
 * Double alternative to android.opengl.Matrix
 * 
 * Matrix math utilities. These methods operate on OpenGL ES format
 * matrices and vectors stored in Double arrays.
 * <p>
 * Matrices are 4 x 4 column-vector matrices stored in column-major
 * order:
 * <pre>
 *  m[offset +  0] m[offset +  4] m[offset +  8] m[offset + 12]
 *  m[offset +  1] m[offset +  5] m[offset +  9] m[offset + 13]
 *  m[offset +  2] m[offset +  6] m[offset + 10] m[offset + 14]
 *  m[offset +  3] m[offset +  7] m[offset + 11] m[offset + 15]</pre>
 *
 * Vectors are 4 x 1 column vectors stored in order:
 * <pre>
 * v[offset + 0]
 * v[offset + 1]
 * v[offset + 2]
 * v[offset + 3]</pre>
 */
open class DoubleMatrix {
    companion object {

    /** Temporary memory for operations that need temporary matrix data. */
    private val ThreadTmp: ThreadLocal<DoubleArray> = object : ThreadLocal<DoubleArray>() {
        override fun initialValue(): DoubleArray = DoubleArray(32)
    }

    private fun overlap(a: DoubleArray, aStart: Int, aLength: Int, b: DoubleArray, bStart: Int, bLength: Int): Boolean {
        if (a != b) {
            return false
        }

        if (aStart == bStart) {
            return true
        }

        var aEnd: Int = aStart + aLength
        var bEnd: Int = bStart + bLength

        if (aEnd == bEnd) {
            return true
        }

        if (aStart < bStart && bStart < aEnd) {
            return true
        }
        if (aStart < bEnd   && bEnd   < aEnd) {
            return true
        }

        if (bStart < aStart && aStart < bEnd) {
            return true
        }
        return bStart < aEnd && aEnd < bEnd
    }

    /**
     * Multiplies two 4x4 matrices together and stores the result in a third 4x4
     * matrix. In matrix notation: result = lhs x rhs. Due to the way
     * matrix multiplication works, the result matrix will have the same
     * effect as first multiplying by the rhs matrix, then multiplying by
     * the lhs matrix. This is the opposite of what you might expect.
     * <p>
     * The same Double array may be passed for result, lhs, and/or rhs. This
     * operation is expected to do the correct thing if the result elements
     * overlap with either of the lhs or rhs elements.
     *
     * @param result The Double array that holds the result.
     * @param resultOffset The offset into the result array where the result is
     *        stored.
     * @param lhs The Double array that holds the left-hand-side matrix.
     * @param lhsOffset The offset into the lhs array where the lhs is stored
     * @param rhs The Double array that holds the right-hand-side matrix.
     * @param rhsOffset The offset into the rhs array where the rhs is stored.
     *
     * @throws IllegalArgumentException under any of the following conditions:
     * result, lhs, or rhs are null;
     * resultOffset + 16 > result.size
     * or lhsOffset + 16 > lhs.size
     * or rhsOffset + 16 > rhs.size;
     * resultOffset < 0 or lhsOffset < 0 or rhsOffset < 0
     */
    @JvmStatic
    fun multiplyMM(result: DoubleArray?, resultOffset: Int, lhs: DoubleArray?, lhsOffset: Int, rhs: DoubleArray?, rhsOffset: Int): Unit {
        // error checking
        if (result == null) {
            throw IllegalArgumentException("result == null")
        }
        if (lhs == null) {
            throw IllegalArgumentException("lhs == null")
        }
        if (rhs == null) {
            throw IllegalArgumentException("rhs == null")
        }
        if (resultOffset < 0) {
            throw IllegalArgumentException("resultOffset < 0")
        }
        if (lhsOffset < 0) {
            throw IllegalArgumentException("lhsOffset < 0")
        }
        if (rhsOffset < 0) {
            throw IllegalArgumentException("rhsOffset < 0")
        }
        if (result.size < resultOffset + 16) {
            throw IllegalArgumentException("result.length < resultOffset + 16")
        }
        if (lhs.size < lhsOffset + 16) {
            throw IllegalArgumentException("lhs.length < lhsOffset + 16")
        }
        if (rhs.size < rhsOffset + 16) {
            throw IllegalArgumentException("rhs.length < rhsOffset + 16")
        }

        // Check for overlap between rhs and result or lhs and result
        if ( overlap(result, resultOffset, 16, lhs, lhsOffset, 16)
                || overlap(result, resultOffset, 16, rhs, rhsOffset, 16) ) {
            var tmp: DoubleArray = ThreadTmp.get()
            for (i in 0 until 4) {
                var rhs_i0: Double = rhs[4 * i + rhsOffset]
                var ri0: Double = lhs[lhsOffset] * rhs_i0
                var ri1: Double = lhs[ 1 + lhsOffset ] * rhs_i0
                var ri2: Double = lhs[ 2 + lhsOffset ] * rhs_i0
                var ri3: Double = lhs[ 3 + lhsOffset ] * rhs_i0
                for (j in 1 until 4) {
                    var rhs_ij: Double = rhs[ 4*i + j + rhsOffset]
                    ri0 += lhs[4 * j + lhsOffset] * rhs_ij
                    ri1 += lhs[ 4*j + 1 + lhsOffset ] * rhs_ij
                    ri2 += lhs[ 4*j + 2 + lhsOffset ] * rhs_ij
                    ri3 += lhs[ 4*j + 3 + lhsOffset ] * rhs_ij
                }
                tmp[4 * i] = ri0
                tmp[ 4*i + 1 ] = ri1
                tmp[ 4*i + 2 ] = ri2
                tmp[ 4*i + 3 ] = ri3
            }

            // copy from tmp to result
            System.arraycopy(tmp, 0, result, 0 + resultOffset, 16)

        } else {
            for (i in 0 until 4) {
                var rhs_i0: Double = rhs[4 * i + rhsOffset]
                var ri0: Double = lhs[lhsOffset] * rhs_i0
                var ri1: Double = lhs[ 1 + lhsOffset ] * rhs_i0
                var ri2: Double = lhs[ 2 + lhsOffset ] * rhs_i0
                var ri3: Double = lhs[ 3 + lhsOffset ] * rhs_i0
                for (j in 1 until 4) {
                    var rhs_ij: Double = rhs[ 4*i + j + rhsOffset]
                    ri0 += lhs[4 * j + lhsOffset] * rhs_ij
                    ri1 += lhs[ 4*j + 1 + lhsOffset ] * rhs_ij
                    ri2 += lhs[ 4*j + 2 + lhsOffset ] * rhs_ij
                    ri3 += lhs[ 4*j + 3 + lhsOffset ] * rhs_ij
                }
                result[4 * i + resultOffset] = ri0
                result[ 4*i + 1 + resultOffset ] = ri1
                result[ 4*i + 2 + resultOffset ] = ri2
                result[ 4*i + 3 + resultOffset ] = ri3
            }
        }
    }

    /**
     * Multiplies a 4 element vector by a 4x4 matrix and stores the result in a
     * 4-element column vector. In matrix notation: result = lhs x rhs
     * <p>
     * The same Double array may be passed for resultVec, lhsMat, and/or rhsVec.
     * This operation is expected to do the correct thing if the result elements
     * overlap with either of the lhs or rhs elements.
     *
     * @param resultVec The Double array that holds the result vector.
     * @param resultVecOffset The offset into the result array where the result
     *        vector is stored.
     * @param lhsMat The Double array that holds the left-hand-side matrix.
     * @param lhsMatOffset The offset into the lhs array where the lhs is stored
     * @param rhsVec The Double array that holds the right-hand-side vector.
     * @param rhsVecOffset The offset into the rhs vector where the rhs vector
     *        is stored.
     *
     * @throws IllegalArgumentException under any of the following conditions:
     * resultVec, lhsMat, or rhsVec are null;
     * resultVecOffset + 4  > resultVec.size
     * or lhsMatOffset + 16 > lhsMat.size
     * or rhsVecOffset + 4  > rhsVec.size;
     * resultVecOffset < 0 or lhsMatOffset < 0 or rhsVecOffset < 0
     */
    @JvmStatic
    fun multiplyMV(resultVec: DoubleArray?, resultVecOffset: Int, lhsMat: DoubleArray?, lhsMatOffset: Int, rhsVec: DoubleArray?, rhsVecOffset: Int): Unit {
        // error checking
        if (resultVec == null) {
            throw IllegalArgumentException("resultVec == null")
        }
        if (lhsMat == null) {
            throw IllegalArgumentException("lhsMat == null")
        }
        if (rhsVec == null) {
            throw IllegalArgumentException("rhsVec == null")
        }
        if (resultVecOffset < 0) {
            throw IllegalArgumentException("resultVecOffset < 0")
        }
        if (lhsMatOffset < 0) {
            throw IllegalArgumentException("lhsMatOffset < 0")
        }
        if (rhsVecOffset < 0) {
            throw IllegalArgumentException("rhsVecOffset < 0")
        }
        if (resultVec.size < resultVecOffset + 4) {
            throw IllegalArgumentException("resultVec.length < resultVecOffset + 4")
        }
        if (lhsMat.size < lhsMatOffset + 16) {
            throw IllegalArgumentException("lhsMat.length < lhsMatOffset + 16")
        }
        if (rhsVec.size < rhsVecOffset + 4) {
            throw IllegalArgumentException("rhsVec.length < rhsVecOffset + 4")
        }

        var tmp0: Double = lhsMat[lhsMatOffset] * rhsVec[rhsVecOffset] +
                lhsMat[4 + lhsMatOffset] * rhsVec[1 + rhsVecOffset] +
                lhsMat[4 * 2 + lhsMatOffset] * rhsVec[2 + rhsVecOffset] +
                lhsMat[4 * 3 + lhsMatOffset] * rhsVec[3 + rhsVecOffset]
        var tmp1: Double = lhsMat[1 + lhsMatOffset] * rhsVec[rhsVecOffset] +
                lhsMat[1 + 4 + lhsMatOffset] * rhsVec[1 + rhsVecOffset] +
                lhsMat[1 + 4 * 2 + lhsMatOffset] * rhsVec[2 + rhsVecOffset] +
                lhsMat[1 + 4 * 3 + lhsMatOffset] * rhsVec[3 + rhsVecOffset]
        var tmp2: Double = lhsMat[2 + lhsMatOffset] * rhsVec[rhsVecOffset] +
                lhsMat[2 + 4 + lhsMatOffset] * rhsVec[1 + rhsVecOffset] +
                lhsMat[2 + 4 * 2 + lhsMatOffset] * rhsVec[2 + rhsVecOffset] +
                lhsMat[2 + 4 * 3 + lhsMatOffset] * rhsVec[3 + rhsVecOffset]
        var tmp3: Double = lhsMat[3 + lhsMatOffset] * rhsVec[rhsVecOffset] +
                lhsMat[3 + 4 + lhsMatOffset] * rhsVec[1 + rhsVecOffset] +
                lhsMat[3 + 4 * 2 + lhsMatOffset] * rhsVec[2 + rhsVecOffset] +
                lhsMat[3 + 4 * 3 + lhsMatOffset] * rhsVec[3 + rhsVecOffset]

        resultVec[resultVecOffset] = tmp0
        resultVec[ 1 + resultVecOffset ] = tmp1
        resultVec[ 2 + resultVecOffset ] = tmp2
        resultVec[ 3 + resultVecOffset ] = tmp3
    }

    /**
     * Transposes a 4 x 4 matrix.
     * <p>
     * mTrans and m must not overlap.
     *
     * @param mTrans the array that holds the output transposed matrix
     * @param mTransOffset an offset into mTrans where the transposed matrix is
     *        stored.
     * @param m the input array
     * @param mOffset an offset into m where the input matrix is stored.
     */
    @JvmStatic
    fun transposeM(mTrans: DoubleArray, mTransOffset: Int, m: DoubleArray, mOffset: Int): Unit {
        for (i in 0 until 4) {
            var mBase: Int = i * 4 + mOffset
            mTrans[i + mTransOffset] = m[mBase]
            mTrans[i + 4 + mTransOffset] = m[mBase + 1]
            mTrans[i + 8 + mTransOffset] = m[mBase + 2]
            mTrans[i + 12 + mTransOffset] = m[mBase + 3]
        }
    }

    /**
     * Inverts a 4 x 4 matrix.
     * <p>
     * mInv and m must not overlap.
     *
     * @param mInv the array that holds the output inverted matrix
     * @param mInvOffset an offset into mInv where the inverted matrix is
     *        stored.
     * @param m the input array
     * @param mOffset an offset into m where the input matrix is stored.
     * @return true if the matrix could be inverted, false if it could not.
     */
    @JvmStatic
    fun invertM(mInv: DoubleArray, mInvOffset: Int, m: DoubleArray, mOffset: Int): Boolean {
        // Invert a 4 x 4 matrix using Cramer's Rule

        // transpose matrix
        var src0: Double = m[mOffset]
        var src4: Double = m[mOffset +  1]
        var src8: Double = m[mOffset +  2]
        var src12: Double = m[mOffset +  3]

        var src1: Double = m[mOffset +  4]
        var src5: Double = m[mOffset +  5]
        var src9: Double = m[mOffset +  6]
        var src13: Double = m[mOffset +  7]

        var src2: Double = m[mOffset +  8]
        var src6: Double = m[mOffset +  9]
        var src10: Double = m[mOffset + 10]
        var src14: Double = m[mOffset + 11]

        var src3: Double = m[mOffset + 12]
        var src7: Double = m[mOffset + 13]
        var src11: Double = m[mOffset + 14]
        var src15: Double = m[mOffset + 15]

        // calculate pairs for first 8 elements (cofactors)
        var atmp0: Double = src10 * src15
        var atmp1: Double = src11 * src14
        var atmp2: Double = src9  * src15
        var atmp3: Double = src11 * src13
        var atmp4: Double = src9  * src14
        var atmp5: Double = src10 * src13
        var atmp6: Double = src8  * src15
        var atmp7: Double = src11 * src12
        var atmp8: Double = src8  * src14
        var atmp9: Double = src10 * src12
        var atmp10: Double = src8  * src13
        var atmp11: Double = src9  * src12

        // calculate first 8 elements (cofactors)
        var dst0: Double = (atmp0 * src5 + atmp3 * src6 + atmp4  * src7) -
                (atmp1 * src5 + atmp2 * src6 + atmp5  * src7)
        var dst1: Double = (atmp1 * src4 + atmp6 * src6 + atmp9  * src7) -
                (atmp0 * src4 + atmp7 * src6 + atmp8  * src7)
        var dst2: Double = (atmp2 * src4 + atmp7 * src5 + atmp10 * src7) -
                (atmp3 * src4 + atmp6 * src5 + atmp11 * src7)
        var dst3: Double = (atmp5 * src4 + atmp8 * src5 + atmp11 * src6) -
                (atmp4 * src4 + atmp9 * src5 + atmp10 * src6)
        var dst4: Double = (atmp1 * src1 + atmp2 * src2 + atmp5  * src3) -
                (atmp0 * src1 + atmp3 * src2 + atmp4  * src3)
        var dst5: Double = (atmp0 * src0 + atmp7 * src2 + atmp8  * src3) -
                (atmp1 * src0 + atmp6 * src2 + atmp9  * src3)
        var dst6: Double = (atmp3 * src0 + atmp6 * src1 + atmp11 * src3) -
                (atmp2 * src0 + atmp7 * src1 + atmp10 * src3)
        var dst7: Double = (atmp4 * src0 + atmp9 * src1 + atmp10 * src2) -
                (atmp5 * src0 + atmp8 * src1 + atmp11 * src2)

        // calculate pairs for second 8 elements (cofactors)
        var btmp0: Double = src2 * src7
        var btmp1: Double = src3 * src6
        var btmp2: Double = src1 * src7
        var btmp3: Double = src3 * src5
        var btmp4: Double = src1 * src6
        var btmp5: Double = src2 * src5
        var btmp6: Double = src0 * src7
        var btmp7: Double = src3 * src4
        var btmp8: Double = src0 * src6
        var btmp9: Double = src2 * src4
        var btmp10: Double = src0 * src5
        var btmp11: Double = src1 * src4

        // calculate second 8 elements (cofactors)
        var dst8: Double = (btmp0  * src13 + btmp3  * src14 + btmp4  * src15) -
                (btmp1  * src13 + btmp2  * src14 + btmp5  * src15)
        var dst9: Double = (btmp1  * src12 + btmp6  * src14 + btmp9  * src15) -
                (btmp0  * src12 + btmp7  * src14 + btmp8  * src15)
        var dst10: Double = (btmp2  * src12 + btmp7  * src13 + btmp10 * src15) -
                (btmp3  * src12 + btmp6  * src13 + btmp11 * src15)
        var dst11: Double = (btmp5  * src12 + btmp8  * src13 + btmp11 * src14) -
                (btmp4  * src12 + btmp9  * src13 + btmp10 * src14)
        var dst12: Double = (btmp2  * src10 + btmp5  * src11 + btmp1  * src9 ) -
                (btmp4  * src11 + btmp0  * src9  + btmp3  * src10)
        var dst13: Double = (btmp8  * src11 + btmp0  * src8  + btmp7  * src10) -
                (btmp6  * src10 + btmp9  * src11 + btmp1  * src8 )
        var dst14: Double = (btmp6  * src9  + btmp11 * src11 + btmp3  * src8 ) -
                (btmp10 * src11 + btmp2  * src8  + btmp7  * src9 )
        var dst15: Double = (btmp10 * src10 + btmp4  * src8  + btmp9  * src9 ) -
                (btmp8  * src9  + btmp11 * src10 + btmp5  * src8 )

        // calculate determinant
        var det: Double =
                src0 * dst0 + src1 * dst1 + src2 * dst2 + src3 * dst3

        if (det == 0.0f.toDouble()) {
            return false
        }

        // calculate matrix inverse
        var invdet: Double = 1.0f.toDouble() / det
        mInv[     mInvOffset] = dst0  * invdet
        mInv[ 1 + mInvOffset] = dst1  * invdet
        mInv[ 2 + mInvOffset] = dst2  * invdet
        mInv[ 3 + mInvOffset] = dst3  * invdet

        mInv[ 4 + mInvOffset] = dst4  * invdet
        mInv[ 5 + mInvOffset] = dst5  * invdet
        mInv[ 6 + mInvOffset] = dst6  * invdet
        mInv[ 7 + mInvOffset] = dst7  * invdet

        mInv[ 8 + mInvOffset] = dst8  * invdet
        mInv[ 9 + mInvOffset] = dst9  * invdet
        mInv[10 + mInvOffset] = dst10 * invdet
        mInv[11 + mInvOffset] = dst11 * invdet

        mInv[12 + mInvOffset] = dst12 * invdet
        mInv[13 + mInvOffset] = dst13 * invdet
        mInv[14 + mInvOffset] = dst14 * invdet
        mInv[15 + mInvOffset] = dst15 * invdet

        return true
    }

    /**
     * Computes an orthographic projection matrix.
     *
     * @param m returns the result
     * @param mOffset
     * @param left
     * @param right
     * @param bottom
     * @param top
     * @param near
     * @param far
     */
    @JvmStatic
    fun orthoM(m: DoubleArray, mOffset: Int, left: Double, right: Double, bottom: Double, top: Double, near: Double, far: Double): Unit {
        if (left == right) {
            throw IllegalArgumentException("left == right")
        }
        if (bottom == top) {
            throw IllegalArgumentException("bottom == top")
        }
        if (near == far) {
            throw IllegalArgumentException("near == far")
        }

        var r_width: Double = 1.0f.toDouble() / (right - left)
        var r_height: Double = 1.0f.toDouble() / (top - bottom)
        var r_depth: Double = 1.0f.toDouble() / (far - near)
        var x: Double =  2.0f.toDouble() * (r_width)
        var y: Double =  2.0f.toDouble() * (r_height)
        var z: Double = -2.0f.toDouble() * (r_depth)
        var tx: Double = -(right + left) * r_width
        var ty: Double = -(top + bottom) * r_height
        var tz: Double = -(far + near) * r_depth
        m[mOffset] = x
        m[mOffset + 5] = y
        m[mOffset +10] = z
        m[mOffset +12] = tx
        m[mOffset +13] = ty
        m[mOffset +14] = tz
        m[mOffset +15] = 1.0
        m[mOffset + 1] = 0.0
        m[mOffset + 2] = 0.0
        m[mOffset + 3] = 0.0
        m[mOffset + 4] = 0.0
        m[mOffset + 6] = 0.0
        m[mOffset + 7] = 0.0
        m[mOffset + 8] = 0.0
        m[mOffset + 9] = 0.0
        m[mOffset + 11] = 0.0
    }


    /**
     * Defines a projection matrix in terms of six clip planes.
     *
     * @param m the Double array that holds the output perspective matrix
     * @param offset the offset into Double array m where the perspective
     *        matrix data is written
     * @param left
     * @param right
     * @param bottom
     * @param top
     * @param near
     * @param far
     */
    @JvmStatic
    fun frustumM(m: DoubleArray, offset: Int, left: Double, right: Double, bottom: Double, top: Double, near: Double, far: Double): Unit {
        if (left == right) {
            throw IllegalArgumentException("left == right")
        }
        if (top == bottom) {
            throw IllegalArgumentException("top == bottom")
        }
        if (near == far) {
            throw IllegalArgumentException("near == far")
        }
        if (near <= 0.0f.toDouble()) {
            throw IllegalArgumentException("near <= 0.0f")
        }
        if (far <= 0.0f.toDouble()) {
            throw IllegalArgumentException("far <= 0.0f")
        }
        var r_width: Double = 1.0f.toDouble() / (right - left)
        var r_height: Double = 1.0f.toDouble() / (top - bottom)
        var r_depth: Double = 1.0f.toDouble() / (near - far)
        var x: Double = 2.0f.toDouble() * (near * r_width)
        var y: Double = 2.0f.toDouble() * (near * r_height)
        var A: Double = (right + left) * r_width
        var B: Double = (top + bottom) * r_height
        var C: Double = (far + near) * r_depth
        var D: Double = 2.0f.toDouble() * (far * near * r_depth)
        m[offset] = x
        m[offset + 5] = y
        m[offset + 8] = A
        m[offset +  9] = B
        m[offset + 10] = C
        m[offset + 14] = D
        m[offset + 11] = -1.0f.toDouble()
        m[offset +  1] = 0.0
        m[offset +  2] = 0.0
        m[offset +  3] = 0.0
        m[offset +  4] = 0.0
        m[offset +  6] = 0.0
        m[offset +  7] = 0.0
        m[offset + 12] = 0.0
        m[offset + 13] = 0.0
        m[offset + 15] = 0.0
    }

    /**
     * Defines a projection matrix in terms of a field of view angle, an
     * aspect ratio, and z clip planes.
     *
     * @param m the Double array that holds the perspective matrix
     * @param offset the offset into Double array m where the perspective
     *        matrix data is written
     * @param fovy field of view in y direction, in degrees
     * @param aspect width to height aspect ratio of the viewport
     * @param zNear
     * @param zFar
     */
    @JvmStatic
    fun perspectiveM(m: DoubleArray, offset: Int, fovy: Double, aspect: Double, zNear: Double, zFar: Double): Unit {
        var f: Double = 1.0f.toDouble() / Math.tan(fovy * (Math.PI / 360.0))
        var rangeReciprocal: Double = 1.0f.toDouble() / (zNear - zFar)

        m[offset] = f / aspect
        m[offset + 1] = 0.0
        m[offset + 2] = 0.0
        m[offset + 3] = 0.0

        m[offset + 4] = 0.0
        m[offset + 5] = f
        m[offset + 6] = 0.0
        m[offset + 7] = 0.0

        m[offset + 8] = 0.0
        m[offset + 9] = 0.0
        m[offset + 10] = (zFar + zNear) * rangeReciprocal
        m[offset + 11] = -1.0f.toDouble()

        m[offset + 12] = 0.0
        m[offset + 13] = 0.0
        m[offset + 14] = 2.0f.toDouble() * zFar * zNear * rangeReciprocal
        m[offset + 15] = 0.0
    }

    /**
     * Computes the length of a vector.
     *
     * @param x x coordinate of a vector
     * @param y y coordinate of a vector
     * @param z z coordinate of a vector
     * @return the length of a vector
     */
    @JvmStatic
    fun length(x: Double, y: Double, z: Double): Double {
        return Math.sqrt(x * x + y * y + z * z)
    }

    /**
     * Sets matrix m to the identity matrix.
     *
     * @param sm returns the result
     * @param smOffset index into sm where the result matrix starts
     */
    @JvmStatic
    fun setIdentityM(sm: DoubleArray, smOffset: Int): Unit {
        for (i in 0 until 16) {
            sm[smOffset + i] = 0.0
        }
        for (i in 0 until 16 step 5) {
            sm[smOffset + i] = 1.0
        }
    }

    /**
     * Scales matrix m by x, y, and z, putting the result in sm.
     * <p>
     * m and sm must not overlap.
     *
     * @param sm returns the result
     * @param smOffset index into sm where the result matrix starts
     * @param m source matrix
     * @param mOffset index into m where the source matrix starts
     * @param x scale factor x
     * @param y scale factor y
     * @param z scale factor z
     */
    @JvmStatic
    fun scaleM(sm: DoubleArray, smOffset: Int, m: DoubleArray, mOffset: Int, x: Double, y: Double, z: Double): Unit {
        for (i in 0 until 4) {
            var smi: Int = smOffset + i
            var mi: Int = mOffset + i
            sm[     smi] = m[     mi] * x
            sm[ 4 + smi] = m[ 4 + mi] * y
            sm[ 8 + smi] = m[ 8 + mi] * z
            sm[12 + smi] = m[12 + mi]
        }
    }

    /**
     * Scales matrix m in place by sx, sy, and sz.
     *
     * @param m matrix to scale
     * @param mOffset index into m where the matrix starts
     * @param x scale factor x
     * @param y scale factor y
     * @param z scale factor z
     */
    @JvmStatic
    fun scaleM(m: DoubleArray, mOffset: Int, x: Double, y: Double, z: Double): Unit {
        for (i in 0 until 4) {
            var mi: Int = mOffset + i
            m[     mi] *= x
            m[ 4 + mi] *= y
            m[ 8 + mi] *= z
        }
    }

    /**
     * Translates matrix m by x, y, and z, putting the result in tm.
     * <p>
     * m and tm must not overlap.
     *
     * @param tm returns the result
     * @param tmOffset index into sm where the result matrix starts
     * @param m source matrix
     * @param mOffset index into m where the source matrix starts
     * @param x translation factor x
     * @param y translation factor y
     * @param z translation factor z
     */
    @JvmStatic
    fun translateM(tm: DoubleArray, tmOffset: Int, m: DoubleArray, mOffset: Int, x: Double, y: Double, z: Double): Unit {
        System.arraycopy(m, mOffset + 0, tm, tmOffset + 0, 12)
        for (i in 0 until 4) {
            var tmi: Int = tmOffset + i
            var mi: Int = mOffset + i
            tm[12 + tmi] = m[mi] * x + m[4 + mi] * y + m[8 + mi] * z +
                    m[12 + mi]
        }
    }

    /**
     * Translates matrix m by x, y, and z in place.
     *
     * @param m matrix
     * @param mOffset index into m where the matrix starts
     * @param x translation factor x
     * @param y translation factor y
     * @param z translation factor z
     */
    @JvmStatic
    fun translateM(m: DoubleArray, mOffset: Int, x: Double, y: Double, z: Double): Unit {
        for (i in 0 until 4) {
            var mi: Int = mOffset + i
            m[12 + mi] += m[mi] * x + m[4 + mi] * y + m[8 + mi] * z
        }
    }

    /**
     * Rotates matrix m by angle a (in degrees) around the axis (x, y, z).
     * <p>
     * m and rm must not overlap.
     *
     * @param rm returns the result
     * @param rmOffset index into rm where the result matrix starts
     * @param m source matrix
     * @param mOffset index into m where the source matrix starts
     * @param a angle to rotate in degrees
     * @param x X axis component
     * @param y Y axis component
     * @param z Z axis component
     */
    @JvmStatic
    fun rotateM(rm: DoubleArray, rmOffset: Int, m: DoubleArray, mOffset: Int, a: Double, x: Double, y: Double, z: Double): Unit {
        var tmp: DoubleArray = ThreadTmp.get()
        setRotateM(tmp, 16, a, x, y, z)
        multiplyMM(rm, rmOffset, m, mOffset, tmp, 16)
    }

    /**
     * Rotates matrix m in place by angle a (in degrees)
     * around the axis (x, y, z).
     *
     * @param m source matrix
     * @param mOffset index into m where the matrix starts
     * @param a angle to rotate in degrees
     * @param x X axis component
     * @param y Y axis component
     * @param z Z axis component
     */
    @JvmStatic
    fun rotateM(m: DoubleArray, mOffset: Int, a: Double, x: Double, y: Double, z: Double): Unit {
        rotateM(m, mOffset, m, mOffset, a, x, y, z)
    }

    /**
     * Creates a matrix for rotation by angle a (in degrees)
     * around the axis (x, y, z).
     * <p>
     * An optimized path will be used for rotation about a major axis
     * (e.g. x=1.0f y=0.0f z=0.0f).
     *
     * @param rm returns the result
     * @param rmOffset index into rm where the result matrix starts
     * @param a angle to rotate in degrees
     * @param x X axis component
     * @param y Y axis component
     * @param z Z axis component
     */
    @JvmStatic
    fun setRotateM(rm: DoubleArray, rmOffset: Int, angleDegrees: Double, axisX: Double, axisY: Double, axisZ: Double): Unit {
        var a = angleDegrees
        var x = axisX
        var y = axisY
        var z = axisZ
        rm[rmOffset + 3] = 0.0
        rm[rmOffset + 7] = 0.0
        rm[rmOffset + 11]= 0.0
        rm[rmOffset + 12]= 0.0
        rm[rmOffset + 13]= 0.0
        rm[rmOffset + 14]= 0.0
        rm[rmOffset + 15]= 1.0
        a *= Math.PI / 180.0f.toDouble()
        var s: Double = Math.sin(a)
        var c: Double = Math.cos(a)
        if (1.0f.toDouble() == x && 0.0f.toDouble() == y && 0.0f.toDouble() == z) {
            rm[rmOffset + 5] = c;   rm[rmOffset + 10]= c
            rm[rmOffset + 6] = s;   rm[rmOffset + 9] = -s
            rm[rmOffset + 1] = 0.0;   rm[rmOffset + 2] = 0.0
            rm[rmOffset + 4] = 0.0;   rm[rmOffset + 8] = 0.0
            rm[rmOffset] = 1.0
        } else if (0.0f.toDouble() == x && 1.0f.toDouble() == y && 0.0f.toDouble() == z) {
            rm[rmOffset] = c;   rm[rmOffset + 10]= c
            rm[rmOffset + 8] = s;   rm[rmOffset + 2] = -s
            rm[rmOffset + 1] = 0.0;   rm[rmOffset + 4] = 0.0
            rm[rmOffset + 6] = 0.0;   rm[rmOffset + 9] = 0.0
            rm[rmOffset + 5] = 1.0
        } else if (0.0f.toDouble() == x && 0.0f.toDouble() == y && 1.0f.toDouble() == z) {
            rm[rmOffset] = c;   rm[rmOffset + 5] = c
            rm[rmOffset + 1] = s;   rm[rmOffset + 4] = -s
            rm[rmOffset + 2] = 0.0;   rm[rmOffset + 6] = 0.0
            rm[rmOffset + 8] = 0.0;   rm[rmOffset + 9] = 0.0
            rm[rmOffset + 10]= 1.0
        } else {
            var len: Double = length(x, y, z)
            if (1.0f.toDouble() != len) {
                var recipLen: Double = 1.0f.toDouble() / len
                x *= recipLen
                y *= recipLen
                z *= recipLen
            }
            var nc: Double = 1.0f.toDouble() - c
            var xy: Double = x * y
            var yz: Double = y * z
            var zx: Double = z * x
            var xs: Double = x * s
            var ys: Double = y * s
            var zs: Double = z * s
            rm[rmOffset] = x*x*nc +  c
            rm[rmOffset +  4] =  xy*nc - zs
            rm[rmOffset +  8] =  zx*nc + ys
            rm[rmOffset +  1] =  xy*nc + zs
            rm[rmOffset +  5] = y*y*nc +  c
            rm[rmOffset +  9] =  yz*nc - xs
            rm[rmOffset +  2] =  zx*nc - ys
            rm[rmOffset +  6] =  yz*nc + xs
            rm[rmOffset + 10] = z*z*nc +  c
        }
    }

    /**
     * Converts Euler angles to a rotation matrix.
     *
     * @param rm returns the result
     * @param rmOffset index into rm where the result matrix starts
     * @param x angle of rotation, in degrees
     * @param y is broken, do not use
     * @param z angle of rotation, in degrees
     *
     * @deprecated This method is incorrect around the y axis. This method is
     *             deprecated and replaced (below) by setRotateEulerM2 which
     *             behaves correctly
     */
    @Deprecated("Incorrect around the y axis; use setRotateEulerM2.")
    @JvmStatic
    fun setRotateEulerM(rm: DoubleArray, rmOffset: Int, xDegrees: Double, yDegrees: Double, zDegrees: Double): Unit {
        var x = xDegrees
        var y = yDegrees
        var z = zDegrees
        x *= Math.PI / 180.0f.toDouble()
        y *= Math.PI / 180.0f.toDouble()
        z *= Math.PI / 180.0f.toDouble()
        var cx: Double = Math.cos(x)
        var sx: Double = Math.sin(x)
        var cy: Double = Math.cos(y)
        var sy: Double = Math.sin(y)
        var cz: Double = Math.cos(z)
        var sz: Double = Math.sin(z)
        var cxsy: Double = cx * sy
        var sxsy: Double = sx * sy

        rm[rmOffset]  =   cy * cz
        rm[rmOffset + 1]  =  -cy * sz
        rm[rmOffset + 2]  =   sy
        rm[rmOffset + 3]  =  0.0f.toDouble()

        rm[rmOffset + 4]  =  cxsy * cz + cx * sz
        rm[rmOffset + 5]  = -cxsy * sz + cx * cz
        rm[rmOffset + 6]  =  -sx * cy
        rm[rmOffset + 7]  =  0.0f.toDouble()

        rm[rmOffset + 8]  = -sxsy * cz + sx * sz
        rm[rmOffset + 9]  =  sxsy * sz + sx * cz
        rm[rmOffset + 10] =  cx * cy
        rm[rmOffset + 11] =  0.0f.toDouble()

        rm[rmOffset + 12] =  0.0f.toDouble()
        rm[rmOffset + 13] =  0.0f.toDouble()
        rm[rmOffset + 14] =  0.0f.toDouble()
        rm[rmOffset + 15] =  1.0f.toDouble()
    }

    /**
     * Converts Euler angles to a rotation matrix.
     *
     * @param rm returns the result
     * @param rmOffset index into rm where the result matrix starts
     * @param x angle of rotation, in degrees
     * @param y angle of rotation, in degrees
     * @param z angle of rotation, in degrees
     *
     * @throws IllegalArgumentException if rm is null;
     * or if rmOffset + 16 > rm.size;
     * rmOffset < 0
     */
    @JvmStatic
    fun setRotateEulerM2(@NonNull rm: DoubleArray, rmOffset: Int, xDegrees: Double, yDegrees: Double, zDegrees: Double): Unit {
        var x = xDegrees
        var y = yDegrees
        var z = zDegrees
        if (rm == null) {
            throw IllegalArgumentException("rm == null")
        }
        if (rmOffset < 0) {
            throw IllegalArgumentException("rmOffset < 0")
        }
        if (rm.size < rmOffset + 16) {
            throw IllegalArgumentException("rm.size < rmOffset + 16")
        }

        x *= Math.PI / 180.0f.toDouble()
        y *= Math.PI / 180.0f.toDouble()
        z *= Math.PI / 180.0f.toDouble()
        var cx: Double = Math.cos(x)
        var sx: Double = Math.sin(x)
        var cy: Double = Math.cos(y)
        var sy: Double = Math.sin(y)
        var cz: Double = Math.cos(z)
        var sz: Double = Math.sin(z)
        var cxsy: Double = cx * sy
        var sxsy: Double = sx * sy

        rm[rmOffset]  =  cy * cz
        rm[rmOffset + 1]  = -cy * sz
        rm[rmOffset + 2]  =  sy
        rm[rmOffset + 3]  =  0.0f.toDouble()

        rm[rmOffset + 4]  =  sxsy * cz + cx * sz
        rm[rmOffset + 5]  = -sxsy * sz + cx * cz
        rm[rmOffset + 6]  = -sx * cy
        rm[rmOffset + 7]  =  0.0f.toDouble()

        rm[rmOffset + 8]  = -cxsy * cz + sx * sz
        rm[rmOffset + 9]  =  cxsy * sz + sx * cz
        rm[rmOffset + 10] =  cx * cy
        rm[rmOffset + 11] =  0.0f.toDouble()

        rm[rmOffset + 12] =  0.0f.toDouble()
        rm[rmOffset + 13] =  0.0f.toDouble()
        rm[rmOffset + 14] =  0.0f.toDouble()
        rm[rmOffset + 15] =  1.0f.toDouble()
    }

    /**
     * Defines a viewing transformation in terms of an eye point, a center of
     * view, and an up vector.
     *
     * @param rm returns the result
     * @param rmOffset index into rm where the result matrix starts
     * @param eyeX eye point X
     * @param eyeY eye point Y
     * @param eyeZ eye point Z
     * @param centerX center of view X
     * @param centerY center of view Y
     * @param centerZ center of view Z
     * @param upX up vector X
     * @param upY up vector Y
     * @param upZ up vector Z
     */
    @JvmStatic
    fun setLookAtM(rm: DoubleArray, rmOffset: Int, eyeX: Double, eyeY: Double, eyeZ: Double, centerX: Double, centerY: Double, centerZ: Double, upX: Double, upY: Double, upZ: Double): Unit {

        // See the OpenGL GLUT documentation for gluLookAt for a description
        // of the algorithm. We implement it in a straightforward way:

        var fx: Double = centerX - eyeX
        var fy: Double = centerY - eyeY
        var fz: Double = centerZ - eyeZ

        // Normalize f
        var rlf: Double = 1.0f.toDouble() / DoubleMatrix.length(fx, fy, fz)
        fx *= rlf
        fy *= rlf
        fz *= rlf

        // compute s = f x up (x means "cross product")
        var sx: Double = fy * upZ - fz * upY
        var sy: Double = fz * upX - fx * upZ
        var sz: Double = fx * upY - fy * upX

        // and normalize s
        var rls: Double = 1.0f.toDouble() / DoubleMatrix.length(sx, sy, sz)
        sx *= rls
        sy *= rls
        sz *= rls

        // compute u = s x f
        var ux: Double = sy * fz - sz * fy
        var uy: Double = sz * fx - sx * fz
        var uz: Double = sx * fy - sy * fx

        rm[rmOffset] = sx
        rm[rmOffset + 1] = ux
        rm[rmOffset + 2] = -fx
        rm[rmOffset + 3] = 0.0

        rm[rmOffset + 4] = sy
        rm[rmOffset + 5] = uy
        rm[rmOffset + 6] = -fy
        rm[rmOffset + 7] = 0.0

        rm[rmOffset + 8] = sz
        rm[rmOffset + 9] = uz
        rm[rmOffset + 10] = -fz
        rm[rmOffset + 11] = 0.0

        rm[rmOffset + 12] = 0.0
        rm[rmOffset + 13] = 0.0
        rm[rmOffset + 14] = 0.0
        rm[rmOffset + 15] = 1.0

        translateM(rm, rmOffset, -eyeX, -eyeY, -eyeZ)
    }
    }
}

