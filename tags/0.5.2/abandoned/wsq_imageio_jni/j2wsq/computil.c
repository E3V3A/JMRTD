/***********************************************************************
      LIBRARY: UTIL - General Purpose Utility Routines

      FILE:    COMPUTIL.C
      AUTHORS: Craig Watson
               Michael Garris
      DATE:    12/24/1999

      Contains general purpose routines responsible for processing
      compression algorithm markers in a compressed datastream.

      ROUTINES:
#cat: getc_skip_marker_segment - skips the segment data following a
#cat:           JPEGB, JPEGL, or WSQ marker in the given memory buffer.

***********************************************************************/
#include <stdio.h>
#include <computil.h>
#include <dataio.h>

/*****************************************************************/
/* Skips the segment data following a JPEGB, JPEGL, or WSQ       */
/* marker in the given memory buffer.                            */
/*****************************************************************/
int getc_skip_marker_segment(const unsigned short marker,
                     unsigned char **cbufptr, unsigned char *ebufptr)
{
   int ret;
   unsigned short length;

   /* Get ushort Length. */
   ret = getc_ushort(&length, cbufptr, ebufptr);

   if(ret)
      return(ret);

   length -= 2;

   /* Check for EOB ... */
   if(((*cbufptr)+length) >= ebufptr){
      fprintf(stderr, "ERROR : getc_skip_marker_segment : ");
      fprintf(stderr, "unexpected end of buffer when parsing ");
      fprintf(stderr, "marker %d segment of length %d\n", marker, length);
      return(-2);
   }

   /* Bump buffer pointer. */
   (*cbufptr) += length;

   return(0);
}

