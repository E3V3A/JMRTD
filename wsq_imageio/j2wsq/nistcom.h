#ifndef _NISTCOM_H
#define _NISTCOM_H

#ifndef _FET_H
#include "fet.h"
typedef FET NISTCOM;
#endif

#define NCM_EXT         "ncm"
#define NCM_HEADER      "NIST_COM"        /* manditory */
#define NCM_PIX_WIDTH   "PIX_WIDTH"       /* manditory */
#define NCM_PIX_HEIGHT  "PIX_HEIGHT"      /* manditory */
#define NCM_PIX_DEPTH   "PIX_DEPTH"       /* 1,8,24 (manditory)*/
#define NCM_PPI         "PPI"             /* -1 if unknown (manditory)*/
#define NCM_COLORSPACE  "COLORSPACE"      /* RGB,YCbCr,GRAY */
#define NCM_N_CMPNTS    "NUM_COMPONENTS"  /* [1..4] (manditory w/hv_factors)*/
#define NCM_HV_FCTRS    "HV_FACTORS"      /* H0,V0:H1,V1:...*/
#define NCM_INTRLV      "INTERLEAVE"      /* 0,1 (manditory w/depth=24) */
#define NCM_COMPRESSION "COMPRESSION"     /* NONE,JPEGB,JPEGL,WSQ */
#define NCM_JPEGB_QUAL  "JPEGB_QUALITY"   /* [20..95] */
#define NCM_JPEGL_PREDICT "JPEGL_PREDICT" /* [1..7] */
#define NCM_WSQ_RATE    "WSQ_BITRATE"     /* ex. .75,2.25 (-1.0 if unknown)*/
#define NCM_LOSSY       "LOSSY"           /* 0,1 */

#define NCM_HISTORY     "HISTORY"         /* ex. SD historical data */
#define NCM_FING_CLASS  "FING_CLASS"      /* ex. A,L,R,S,T,W */
#define NCM_SEX         "SEX"             /* m,f */
#define NCM_SCAN_TYPE   "SCAN_TYPE"       /* l,i */
#define NCM_FACE_POS    "FACE_POS"        /* f,p */
#define NCM_AGE         "AGE"
#define NCM_SD_ID       "SD_ID"           /* 4,9,10,14,18 */


/* nistcom.c */
extern int combine_nistcom(NISTCOM **, const int, const int,
                           const int, const int, const int);
extern int combine_wsq_nistcom(NISTCOM **, const int, const int,
                           const int, const int, const int, const float);
extern int del_wsq_nistcom(NISTCOM *);
extern int add_wsq_nistcom(NISTCOM *);

#endif /* !_NISTCOM_H */
