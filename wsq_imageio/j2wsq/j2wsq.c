/**
 * Java wrapper around the FFPIS project's WSQ interpretation stuff.
 * FFPIS project by Garris & Watson of NIST.
 * See http://sourceforge.net.
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
 * Based on dwsq.c.
 */
JNIEXPORT jobject JNICALL Java_WSQ_decodeWSQ (JNIEnv *env,
                                                 jobject obj,
                                                 jbyteArray in) {

   char *j_str;
   unsigned char *idata, *odata;
   int i;

   jsize ilen, olen;
   jbyte *jidata, *jodata;
   jint jwidth, jheight, jdepth, jimgtype;
   jbyteArray jdecodedbytes;
   jclass imgClazz, rasterClazz;
   jobject imgObject, rasterObject;
   jmethodID imgInitMethodID, imgGetRasterMethodID, rasterSetDataElementsMethodID;
   jfieldID imgTypeByteGrayFieldID;

   int ret;
   int width, height, depth, ppi; /* image parameters */
   int lossyflag;                 /* data loss flag */
   NISTCOM *nistcom;              /* NIST Comment */
   char *ppi_str;

   ilen = (*env)->GetArrayLength(env, in);
   jidata = (*env)->GetByteArrayElements(env, in, JNI_FALSE);
   idata = (unsigned char *)malloc(sizeof(unsigned char) * ilen);

   /* Convert input from jbyte to unsigned char. */
   for (i = 0; i < ilen; i++) {
      idata[i] = (unsigned char)(jidata[i]);
   }

   ret = wsq_decode_mem(&odata, &width, &height, &depth, &ppi, &lossyflag, idata, ilen);
   if(ret){
      // free(idata);
      (*env)->ReleaseByteArrayElements(env, in, idata, JNI_FALSE);
      exit(ret); // FIXME: exception? Use ThrowNew(env, clazz, message) (\return 0 on succes, < 0 on failure)
   }

   /* Get NISTCOM from compressed data. */
   ret = getc_nistcom_wsq(&nistcom, idata, ilen);
   if(ret){
      free(idata);
      (*env)->ReleaseByteArrayElements(env, in, jidata, JNI_FALSE);
      exit(ret);
   }
   free(idata);
   (*env)->ReleaseByteArrayElements(env, in, jidata, JNI_FALSE);

   /* WSQ decoder always returns ppi=-1, so believe PPI in NISTCOM, */
   /* if it already exists. */
   ppi_str = (char *)NULL;
   if(nistcom != (NISTCOM *)NULL){
      ret = extractfet_ret(&ppi_str, NCM_PPI, nistcom);
      if(ret){
         free(odata);
         freefet(nistcom);
         exit(ret); // TODO: exception?
      }
   }
   if(ppi_str != (char *)NULL){
      ppi = atoi(ppi_str);
      free(ppi_str);
   }

   /* Combine NISTCOM with image features */
   ret = combine_wsq_nistcom(&nistcom, width, height, depth, ppi, lossyflag,
		   0.0 /* NOTE: will be deleted next */);
   if(ret) {
     free(odata);
     if(nistcom != (NISTCOM *)NULL) {
        freefet(nistcom);
     }
     exit(ret); // FIXME: exception?
   }

   // (Re)read image features from comment.
   ret = extractfet_ret(&j_str, "PIX_WIDTH", nistcom);
   if (ret) { /* FIXME: exception? */ }
   jwidth = atoi(j_str);
   ret = extractfet_ret(&j_str, "PIX_HEIGHT", nistcom);
   if (ret) { /* FIXME: exception? */ }
   jheight = atoi(j_str);
   ret = extractfet_ret(&j_str, "PIX_DEPTH", nistcom);
   if (ret) { /* FIXME: exception? */ }
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
   imgTypeByteGrayFieldID = (*env)->GetStaticFieldID(env, imgClazz, "TYPE_BYTE_GRAY", "I");
   rasterClazz = (*env)->FindClass(env, "java/awt/image/WritableRaster");
   imgInitMethodID = (*env)->GetMethodID(env, imgClazz, "<init>", "(III)V");
   imgGetRasterMethodID = (*env)->GetMethodID(env, imgClazz, "getRaster", "()Ljava/awt/image/WritableRaster;");
   rasterSetDataElementsMethodID = (*env)->GetMethodID(env, rasterClazz, "setDataElements", "(IIIILjava/lang/Object;)V");
   jimgtype = (*env)->GetStaticIntField(env, imgClazz, imgTypeByteGrayFieldID);
   imgObject = (*env)->NewObject(env, imgClazz, imgInitMethodID, jwidth, jheight, jimgtype);
   rasterObject = (*env)->CallObjectMethod(env, imgObject, imgGetRasterMethodID);
   (*env)->CallVoidMethod(env, rasterObject, rasterSetDataElementsMethodID, 0, 0, jwidth, jheight, jdecodedbytes);

   /* Return. */
   return imgObject;
}

