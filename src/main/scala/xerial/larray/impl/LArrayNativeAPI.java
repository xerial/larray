//--------------------------------------
//
// LArrayNativeAPI.scala
// Since: 2013/03/18 1:10 PM
//
//--------------------------------------

package xerial.larray.impl;

/**
 * API for accessing native codes
 * @author Taro L. Saito
 */
public interface LArrayNativeAPI {
    public int copyToArray(long srcAddress, Object destArray, int destOffset, int length);
    public int copyFromArray(Object srcArray, int srcOffset, long destAddress, int length);
}
