/**
 * Java wrapper around the FFPIS project's WSQ interpretation stuff.
 * FFPIS project by Garris & Watson of NIST.
 * See http://ffpis.sourceforge.net.
 *
 * This should be compiled to libj2wsq.so (on linux) or j2wsq.dll (on win32).
 */

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <wsq.h>
#include <ioutil.h>

int debug = -1;

/**
 * Decodes an encoded WSQ byte array to a buffered image.
 * Based on dwsq.c.
 *
 * @param in a byte array holding the encoded WSQ image
 *
 * @return a buffered image
 */
JNIEXPORT jobject JNICALL Java_org_jmrtd_imageio_WSQImageReader_decodeWSQ
 (JNIEnv *env, jobject obj, jbyteArray in, jobject metadata) {
   /* Java classes and objects */
  jclass    ioexceptionClazz = (*env)->FindClass(env, "java/io/IOException");
  jclass    metadataClazz = (*env)->GetObjectClass(env, metadata);
  jclass    imgClazz = (*env)->FindClass(env, "java/awt/image/BufferedImage");
  jclass    writableRasterClazz = (*env)->FindClass(env, "java/awt/image/WritableRaster");
  jmethodID metadataSetPropertyMethodID = (*env)->GetMethodID(env, metadataClazz, "setProperty", "(Ljava/lang/String;Ljava/lang/String;)Z");  
  jmethodID imgInitMethodID = (*env)->GetMethodID(env, imgClazz, "<init>", "(III)V");
  jmethodID imgGetRasterMethodID = (*env)->GetMethodID(env, imgClazz, "getRaster", "()Ljava/awt/image/WritableRaster;");
  jmethodID rasterSetDataElementsMethodID = (*env)->GetMethodID(env, writableRasterClazz, "setDataElements", "(IIIILjava/lang/Object;)V");
  jfieldID  imgTypeByteGrayFieldID = (*env)->GetStaticFieldID(env, imgClazz, "TYPE_BYTE_GRAY", "I");
  jint      TYPE_BYTE_GRAY = (*env)->GetStaticIntField(env, imgClazz, imgTypeByteGrayFieldID);
  jobject   imgObject, rasterObject;    
  
  /* WSQ-Compressed input buffer */
  unsigned char *idata = (unsigned char*)(*env)->GetByteArrayElements(env, in, JNI_FALSE);
  int idata_len = (*env)->GetArrayLength(env, in);
  
  /* Uncompressed Pixel Buffer */
  unsigned char *odata = NULL;
  jbyteArray jodata = NULL;
  /* Image parameters */
  NISTCOM *nistcom = NULL;
  int width, height, depth, ppi, lossy; 
  int wsq_decore_ret, nistcom_ret;
  /* Parse WSQ Image */
  wsq_decore_ret = wsq_decode_mem(&odata, &width, &height, &depth, &ppi, &lossy, idata, idata_len);
  if (wsq_decore_ret==0) {
    jodata = (*env)->NewByteArray(env, width*height);
    (*env)->SetByteArrayRegion(env, jodata, 0, width*height, (jbyte*)odata);
    free(odata);
    odata = NULL;
  } else {
    (*env)->ReleaseByteArrayElements(env, in, (jbyte*)idata, JNI_FALSE);
    (*env)->ThrowNew(env, ioexceptionClazz, "(In native C code) function wsq_decode_mem failed");
    return NULL;
  }
  
  
  /* WSQ Metadata */
  
  nistcom_ret    = getc_nistcom_wsq(&nistcom, idata, idata_len);
  if (nistcom_ret==0 && nistcom != NULL) {
    int i;
    for (i=0; i<nistcom->num; i++) {
      if (nistcom->names[i]) {
        (*env)->CallBooleanMethod(
          env, 
          metadata, 
          metadataSetPropertyMethodID,
          (*env)->NewStringUTF(env, nistcom->names[i]), 
          nistcom->values[i] ? (*env)->NewStringUTF(env, nistcom->values[i]) : NULL );
      }
    }
    freefet(nistcom);
  }
  (*env)->ReleaseByteArrayElements(env, in, (jbyte*)idata, JNI_FALSE);
  
  
  /* Construct a BufferedImage from the byte array:
      BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
      WritableRaster raster = img.getRaster();
      raster.setDataElements(0, 0, width, height, decodedBytes); 
  */
  imgObject = (*env)->NewObject(env, imgClazz, imgInitMethodID, width, height, TYPE_BYTE_GRAY);
  rasterObject = (*env)->CallObjectMethod(env, imgObject, imgGetRasterMethodID);
  (*env)->CallVoidMethod(env, rasterObject, rasterSetDataElementsMethodID, 0, 0, width, height, jodata);
 
  return imgObject;
}

/**
 * Encodes a buffered image as a WSQ byte array.
 * Based on cwsq.c.
 *
 * @param imgObject the buffered image to encode/compress
 * @param r_bitrate the target bit rate
 * @param ppi the target density
 *
 * @return a byte array containing the encoded image
 */
JNIEXPORT jbyteArray JNICALL Java_org_jmrtd_imageio_WSQImageWriter_encodeWSQ
 (JNIEnv *env, jobject obj, jobject imgObject, jdouble bitrate, jint ppi, jstring jnistcom_text) {
   jclass ioexceptionClazz                 = (*env)->FindClass(env, "java/io/IOException");
   jclass imgClazz                         = (*env)->FindClass(env, "java/awt/image/BufferedImage");
   jclass rasterClazz                      = (*env)->FindClass(env, "java/awt/image/Raster");
   jclass writableRasterClazz              = (*env)->FindClass(env, "java/awt/image/WritableRaster");
   jmethodID imgGetRasterMethodID          = (*env)->GetMethodID(env, imgClazz, "getRaster", "()Ljava/awt/image/WritableRaster;");
   jmethodID imgGetWidthMethodID           = (*env)->GetMethodID(env, imgClazz, "getWidth", "()I");
   jmethodID imgGetHeightMethodID          = (*env)->GetMethodID(env, imgClazz, "getHeight", "()I");
   jmethodID rasterGetDataElementsMethodID = (*env)->GetMethodID(env, rasterClazz, "getDataElements", "(IIIILjava/lang/Object;)Ljava/lang/Object;");

   /* Get width, height from buffered img. */
   jint width = (*env)->CallIntMethod(env, imgObject, imgGetWidthMethodID);
   jint height = (*env)->CallIntMethod(env, imgObject, imgGetHeightMethodID);

   /* Copy pixel data from buffered img to unsigned char array. */
   jobject        rasterObject = (*env)->CallObjectMethod(env, imgObject, imgGetRasterMethodID);
   jbyteArray     jpixels      = (jbyteArray)((*env)->CallObjectMethod(env, rasterObject, rasterGetDataElementsMethodID, 0, 0, width, height, NULL));
   jint           ilen         = (*env)->GetArrayLength(env, jpixels);
   unsigned char* pixels       = (unsigned char*)(*env)->GetByteArrayElements(env, jpixels, JNI_FALSE);

   char* nistcom_text = (char*)(*env)->GetStringUTFChars(env, jnistcom_text, NULL);
   
   unsigned char* odata=NULL;
   jbyteArray jout = NULL;
   int olen=0;
   
   int encode_ret = wsq_encode_mem(&odata, &olen, (float)bitrate, pixels, width, height, 8, ppi, nistcom_text);
   
   (*env)->ReleaseByteArrayElements(env, jpixels, (jbyte*)pixels, JNI_FALSE);
   (*env)->ReleaseStringUTFChars(env, jnistcom_text, nistcom_text);
   
   if (encode_ret){
      (*env)->ThrowNew(env, ioexceptionClazz, "(In native C code) function wsq_encode_mem failed");
      return NULL;
   }
   jout = (*env)->NewByteArray(env, olen);
   (*env)->SetByteArrayRegion(env, jout, 0, olen, (jbyte*)odata);
   free(odata);
   return jout;
}

