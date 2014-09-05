	/***********************************************************************
      LIBRARY: FET - Feature File/List Utilities

      FILE:    NISTCOM.C
      AUTHORS: Michael Garris
               Craig Watson
      DATE:    02/27/2001

      Contains routines responsible for manipulating a specialize
      attribute-value list called a NISTCOM used to hold image
      attributes relevant to image compression, decompression, and
      classification.

      ROUTINES:
#cat: combine_nistcom - takes an initialized FET NISTCOM structure
#cat:             or allocates one if necessary, and updates the
#cat:             structure with general image attributes.
#cat: combine_wsq_nistcom - takes an initialized FET NISTCOM structure
#cat:             or allocates one if necessary, and updates the
#cat:             structure with WSQ-specific image attributes.
#cat: del_wsq_nistcom - takes an initialized FET NISTCOM structure
#cat:             and removes WSQ compression attributes.
#cat:

***********************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <nistcom.h>

/*****************************************************************/
int combine_nistcom(NISTCOM **onistcom, const int w, const int h,
                     const int d, const int ppi, const int lossyflag)
{
   int ret, allocflag, n;
   char *lossyval;
   char cbuff[11];
   NISTCOM *nistcom;


   /* ALLOCATION ? */
   if((*onistcom) == (NISTCOM *)NULL){
      ret = allocfet_ret(&nistcom, 6);
      if(ret)
         return(ret);
      allocflag = 1;
      /* HEADER */
      ret = updatefet_ret(NCM_HEADER, "6", nistcom);
      if(ret){
         if(allocflag){
            freefet(nistcom);
            *onistcom = (NISTCOM *)NULL;
         }
         return(ret);
      }
   }
   else{
      nistcom = *onistcom;
      allocflag = 0;
      n = 6;
   }

   /* WIDTH */
   sprintf(cbuff, "%d", w);
   ret = updatefet_ret(NCM_PIX_WIDTH, cbuff, nistcom);
   if(ret){
      if(allocflag){
         freefet(nistcom);
         *onistcom = (NISTCOM *)NULL;
      }
      return(ret);
   }

   /* HEIGHT */
   sprintf(cbuff, "%d", h);
   ret = updatefet_ret(NCM_PIX_HEIGHT, cbuff, nistcom);
   if(ret){
      if(allocflag){
         freefet(nistcom);
         *onistcom = (NISTCOM *)NULL;
      }
      return(ret);
   }

   /* DEPTH */
   sprintf(cbuff, "%d", d);
   ret = updatefet_ret(NCM_PIX_DEPTH, cbuff, nistcom);
   if(ret){
      if(allocflag){
         freefet(nistcom);
         *onistcom = (NISTCOM *)NULL;
      }
      return(ret);
   }

   /* PPI */
   sprintf(cbuff, "%d", ppi);
   ret = updatefet_ret(NCM_PPI, cbuff, nistcom);
   if(ret){
      if(allocflag){
         freefet(nistcom);
         *onistcom = (NISTCOM *)NULL;
      }
      return(ret);
   }

   /* LOSSY */
   /* If exists, lookup current LOSSY value. */
   ret = lookupfet(&lossyval, NCM_LOSSY, nistcom);
   /* If error ... */
   if(ret < 0){
      if(allocflag){
         freefet(nistcom);
         *onistcom = (NISTCOM *)NULL;
      }
      return(ret);
   }
   /* If LOSSY value found AND is set AND requesting to unset ... */
   if(ret &&
     (strcmp(lossyval,"0") != 0) &&
     (lossyflag == 0)){
      fprintf(stderr, "WARNING : combine_nistcom : ");
      fprintf(stderr, "request to unset lossy flag ignored\n");
   }
   else{
      sprintf(cbuff, "%d", lossyflag);
      ret = updatefet_ret(NCM_LOSSY, cbuff, nistcom);
      if(ret){
         if(allocflag){
            freefet(nistcom);
            *onistcom = (NISTCOM *)NULL;
         }
         return(ret);
      }
   }

   /* UPDATE HEADER */
   sprintf(cbuff, "%d", nistcom->num);
   ret = updatefet_ret(NCM_HEADER, cbuff, nistcom);
   if(ret){
      if(allocflag){
         freefet(nistcom);
         *onistcom = (NISTCOM *)NULL;
      }
      return(ret);
   }

   *onistcom = nistcom;

   return(0);
}

/*****************************************************************/
int combine_wsq_nistcom(NISTCOM **onistcom, const int w, const int h,
                  const int d, const int ppi, const int lossyflag,
                  const float r_bitrate)
{
   int ret, allocflag;
   NISTCOM *nistcom;
   char cbuff[MAXFETLENGTH];

   if(*onistcom == (NISTCOM *)NULL)
      allocflag = 1;
   else
      allocflag = 0;

   /* Combine image attributes to NISTCOM. */
   ret = combine_nistcom(onistcom, w, h, d, ppi, lossyflag);
   if(ret)
      return(ret);

   nistcom = *onistcom;

   /* COLORSPACE */
   ret = updatefet_ret(NCM_COLORSPACE, "GRAY", nistcom);
   if(ret){
      if(allocflag)
         freefet(nistcom);
      return(ret);
   }

   /* COMPRESSION */
   ret = updatefet_ret(NCM_COMPRESSION, "WSQ", nistcom);
   if(ret){
      if(allocflag)
         freefet(nistcom);
      return(ret);
   }

   /* BITRATE */
   sprintf(cbuff, "%f", r_bitrate);
   ret = updatefet_ret(NCM_WSQ_RATE, cbuff, nistcom);
   if(ret){
      if(allocflag)
         freefet(nistcom);
      return(ret);
   }

   /* UPDATE HEADER */
   sprintf(cbuff, "%d", nistcom->num);
   ret = updatefet_ret(NCM_HEADER, cbuff, nistcom);
   if(ret){
      if(allocflag)
         freefet(nistcom);
      return(ret);
   }

   return(0);
}

/*****************************************************************/
int del_wsq_nistcom(NISTCOM *nistcom)
{
   int ret;
   char cbuff[MAXFETLENGTH];

   /* COMPRESSION */
   ret = deletefet_ret(NCM_COMPRESSION, nistcom);
   if(ret)
      return(ret);

   /* BITRATE */
   ret = deletefet_ret(NCM_WSQ_RATE, nistcom);
   if(ret)
      return(ret);

   /* UPDATE HEADER */
   sprintf(cbuff, "%d", nistcom->num);
   ret = updatefet_ret(NCM_HEADER, cbuff, nistcom);
   if(ret)
      return(ret);

   return(0);
}

