/***********************************************************************
      LIBRARY: WSQ - Grayscale Image Compression

      FILE:    PPI.C
      AUTHORS: Craig Watson
               Michael Garris
      DATE:    01/17/2001

      Contains routines responsible for determining the scan
      resolution of a WSQ compressed image by attempting to
      locate and parse a NISTCOM comment in the datastream.

      ROUTINES:
#cat: getc_ppi_wsq - Given a WSQ compressed data stream, attempts to
#cat:                read a NISTCOM comment from a memory buffer and
#cat:                if possible return the pixel scan resulution
#cat:                (PPI value) stored therein.

***********************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <wsq.h>
#include <fet.h>

/************************************************************************/
int getc_ppi_wsq(int *oppi, unsigned char *idata, const int ilen)
{
   int ret;
   int ppi;
   char *value;
   NISTCOM *nistcom;

   /* Get ppi from NISTCOM, if one exists ... */
   ret = getc_nistcom_wsq(&nistcom, idata, ilen);
   if(ret)
      return(ret);
   if(nistcom != (NISTCOM *)NULL){
      ret = extractfet_ret(&value, NCM_PPI, nistcom);
      if(ret){
         freefet(nistcom);
         return(ret);
      }
      if(value != (char *)NULL){
         ppi = atoi(value);
         free(value);
      }
      /* Otherwise, PPI not in NISTCOM, so ppi = -1. */
      else
         ppi = -1;
      freefet(nistcom);
   }
   /* Otherwise, NISTCOM does NOT exist, so ppi = -1. */
   else
      ppi = -1;

   *oppi = ppi;

   return(0);
}
