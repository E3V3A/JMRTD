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

int debug = 0;

/**
 * Decodes an encoded WSQ byte array to a buffered image.
 * Based on dwsq.c.
 *
 * @param in a byte array holding the encoded WSQ image
 *
 * @return a buffered image
 */
JNIEXPORT jobject JNICALL Java_org_jmrtd_imageio_WSQImageReader_decodeWSQ
 (JNIEnv *env, jobject obj, jbyteArray in) {
   char *j_str;
   unsigned char *idata, *odata;

   jsize ilen, olen;
   jbyte *jidata, *jodata;
   jint jwidth, jheight, jdepth, jimgtype;
   jbyteArray jdecodedbytes;
   jclass imgClazz, writableRasterClazz, ioexceptionClazz;
   jobject imgObject, rasterObject;
   jmethodID imgInitMethodID, imgGetRasterMethodID, rasterSetDataElementsMethodID;
   jfieldID imgTypeByteGrayFieldID;

   int i, ret;
   int width, height, depth, ppi; /* image parameters */
   int lossyflag;                 /* data loss flag */
   NISTCOM *nistcom;              /* NIST Comment */
   char *ppi_str;

   /* Just in case we want to throw something (at someone)... */
   ioexceptionClazz = (*env)->FindClass(env, "java/io/IOException");

   ilen = (*env)->GetArrayLength(env, in);
   jidata = (*env)->GetByteArrayElements(env, in, JNI_FALSE);
   idata = (unsigned char *)malloc(sizeof(unsigned char) * ilen);

   /* Convert input from jbyte to unsigned char. */
   for (i = 0; i < ilen; i++) {
      idata[i] = (unsigned char)(jidata[i]);
   }
   (*env)->ReleaseByteArrayElements(env, in, jidata, JNI_FALSE);

   ret = wsq_decode_mem(&odata, &width, &height, &depth, &ppi, &lossyflag, idata, ilen);
   if (ret){
      free(idata);
      (*env)->ThrowNew(env, ioexceptionClazz, "(In native C code) function wsq_decode_mem failed");
      return NULL;
   }

   /* Get NISTCOM from compressed data. */
   ret = getc_nistcom_wsq(&nistcom, idata, ilen);
   if (ret){
      free(idata);
      (*env)->ThrowNew(env, ioexceptionClazz, "(In native C code) function getc_nistcom_wsq failed");
      return NULL;
   }
   free(idata);

   /* WSQ decoder always returns ppi=-1, so believe PPI in NISTCOM, */
   /* if it already exists. */
   ppi_str = (char *)NULL;
   if (nistcom != (NISTCOM *)NULL){
      ret = extractfet_ret(&ppi_str, NCM_PPI, nistcom);
      if (ret){
         free(odata);
         freefet(nistcom);
         (*env)->ThrowNew(env, ioexceptionClazz, "(In native C code) function extractfet_ret failed on NCM_PPI");
         return NULL;
      }
   }
   if (ppi_str != (char *)NULL){
      ppi = atoi(ppi_str);
      free(ppi_str);
   }

   /* Combine NISTCOM with image features */
   ret = combine_wsq_nistcom(&nistcom, width, height, depth, ppi, lossyflag,
		   0.0 /* NOTE: will be deleted next */);
   if (ret) {
     free(odata);
     if (nistcom != (NISTCOM *)NULL) {
        freefet(nistcom);
     }
     (*env)->ThrowNew(env, ioexceptionClazz, "(In native C code) function combine_wsq_nistcom failed");
     return NULL;
   }

   // (Re)read image features from comment.
   ret = extractfet_ret(&j_str, NCM_PIX_WIDTH, nistcom);
   if (ret) {
      (*env)->ThrowNew(env, ioexceptionClazz, "(In native C code) function extractfet_ret failed on NCM_PIX_WIDTH");
      return NULL;
   }
   jwidth = atoi(j_str);
   ret = extractfet_ret(&j_str, NCM_PIX_HEIGHT, nistcom);
   if (ret) {
      (*env)->ThrowNew(env, ioexceptionClazz, "(In native C code) function extractfet_ret failed on NCM_PIX_HEIGHT");
      return NULL;
   }
   jheight = atoi(j_str);
   ret = extractfet_ret(&j_str, NCM_PIX_DEPTH, nistcom);
   if (ret) {
      (*env)->ThrowNew(env, ioexceptionClazz, "(In native C code) function extractfet_ret failed on NCM_PIX_DEPTH");
      return NULL;
   }
   jdepth = atoi(j_str);

   freefet(nistcom);

   olen = jwidth * jheight * (depth / 8);

   /* Convert output from unsigned char to jbyte. */
   jodata = malloc(sizeof(jbyte) * olen);
   for (i = 0; i < olen; i++) {
      jodata[i] = (jbyte)(odata[i]);
   }
   free(odata);

   /* Copy output to Java array. */
   jdecodedbytes = (*env)->NewByteArray(env, olen);
   (*env)->SetByteArrayRegion(env, jdecodedbytes, 0, olen, jodata);

   /* Construct a BufferedImage from the byte array.
    *
    * BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
    * WritableRaster raster = img.getRaster();
    * raster.setDataElements(0, 0, width, height, decodedBytes);
    */

   imgClazz = (*env)->FindClass(env, "java/awt/image/BufferedImage");
   writableRasterClazz = (*env)->FindClass(env, "java/awt/image/WritableRaster");
   imgTypeByteGrayFieldID = (*env)->GetStaticFieldID(env, imgClazz, "TYPE_BYTE_GRAY", "I");
   imgInitMethodID = (*env)->GetMethodID(env, imgClazz, "<init>", "(III)V");
   imgGetRasterMethodID = (*env)->GetMethodID(env, imgClazz, "getRaster", "()Ljava/awt/image/WritableRaster;");
   rasterSetDataElementsMethodID = (*env)->GetMethodID(env, writableRasterClazz, "setDataElements", "(IIIILjava/lang/Object;)V");
   jimgtype = (*env)->GetStaticIntField(env, imgClazz, imgTypeByteGrayFieldID);
   imgObject = (*env)->NewObject(env, imgClazz, imgInitMethodID, jwidth, jheight, jimgtype);
   rasterObject = (*env)->CallObjectMethod(env, imgObject, imgGetRasterMethodID);
   (*env)->CallVoidMethod(env, rasterObject, rasterSetDataElementsMethodID, 0, 0, jwidth, jheight, jdecodedbytes);

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
 (JNIEnv *env, jobject obj, jobject imgObject, jdouble r_bitrate, jint ppi) {
   int i, ret;
   jint jwidth, jheight, jdepth;    /* Image characteristic parameters */
   unsigned char *idata, *odata;    /* Input and output data */
   jsize ilen, olen;                /* Number of bytes in input and output data. */
   char *comment_text;
   jbyte *jidata, *jodata;
   jbyteArray in, out;
   jclass imgClazz, rasterClazz, writableRasterClazz, ioexceptionClazz;
   jobject rasterObject;
   jmethodID imgGetWidthMethodID, imgGetHeightMethodID, imgGetRasterMethodID, rasterGetDataElementsMethodID;

   imgClazz = (*env)->FindClass(env, "java/awt/image/BufferedImage");
   rasterClazz = (*env)->FindClass(env, "java/awt/image/Raster");
   writableRasterClazz = (*env)->FindClass(env, "java/awt/image/WritableRaster");
   imgGetRasterMethodID = (*env)->GetMethodID(env, imgClazz, "getRaster", "()Ljava/awt/image/WritableRaster;");
   imgGetWidthMethodID = (*env)->GetMethodID(env, imgClazz, "getWidth", "()I");
   imgGetHeightMethodID = (*env)->GetMethodID(env, imgClazz, "getHeight", "()I");
   imgGetRasterMethodID = (*env)->GetMethodID(env, imgClazz, "getRaster", "()Ljava/awt/image/WritableRaster;");
   rasterGetDataElementsMethodID = (*env)->GetMethodID(env, rasterClazz, "getDataElements", "(IIIILjava/lang/Object;)Ljava/lang/Object;");

   /* Get width, height from buffered img. */
   jwidth = (*env)->CallIntMethod(env, imgObject, imgGetWidthMethodID);
   jheight = (*env)->CallIntMethod(env, imgObject, imgGetHeightMethodID);
   jdepth = 8;

   /* FIXME: check image type -> should be 8 bit gray scale, otherwise throw exception. */

   /* Copy pixel data from buffered img to unsigned char array. */
   rasterObject = (*env)->CallObjectMethod(env, imgObject, imgGetRasterMethodID);
   in = (jbyteArray)((*env)->CallObjectMethod(env, rasterObject, rasterGetDataElementsMethodID, 0, 0, jwidth, jheight, NULL));
   ilen = (*env)->GetArrayLength(env, in);
   jidata = (*env)->GetByteArrayElements(env, in, JNI_FALSE);
   idata = (unsigned char *)malloc(sizeof(unsigned char) * ilen);
   for (i = 0; i < ilen; i++) {
      idata[i] = (unsigned char)(jidata[i]);
   }
   (*env)->ReleaseByteArrayElements(env, in, jidata, JNI_FALSE);

   /* Encode/compress the image pixmap. */
   ret = wsq_encode_mem(&odata, &olen, (float)r_bitrate,
                   idata, jwidth, jheight, jdepth, ppi, comment_text);
   if (ret){
      free(idata);
      if (comment_text != (char *)NULL) { free(comment_text); }
      (*env)->ThrowNew(env, ioexceptionClazz, "(In native C code) function wsq_encode_mem failed");
      return NULL;
   }
   free(idata);
   if (comment_text != (char *)NULL) { free(comment_text); }

   /* Convert output from unsigned char array to jbyteArray. */
   jodata = malloc(sizeof(jbyte) * olen);
   for (i = 0; i < olen; i++) {
      jodata[i] = (jbyte)(odata[i]);
   }
   free(odata);
   out = (*env)->NewByteArray(env, olen);
   (*env)->SetByteArrayRegion(env, out, 0, olen, jodata);

   return out;
}

