/***********************************************************************
      LIBRARY: IOUTIL - INPUT/OUTPUT Utilities

      FILE:    DATAIO.C
      AUTHORS: Craig Watson
               Michael Garris
      DATE:    11/24/1999

      Contains fundamental routines for reading and writing data
      to/from either an open file or memory buffer.

      ROUTINES:
#cat: getc_byte - Reads a byte of data from a memory buffer.
#cat:
#cat: getc_bytes - Reads a sequence of data bytes from a memory buffer.
#cat:
#cat: putc_byte - Writes a byte of data to a memory buffer.
#cat:
#cat: putc_byte - Writes a sequence of data bytes to a memory buffer.
#cat:
#cat: getc_ushort - Reads an unsigned short from a memory buffer.
#cat:
#cat: putc_ushort - Writes an unsigned short to a memory buffer.
#cat:
#cat: getc_uint - Reads an unsigned integer from a memory buffer.
#cat:
#cat: putc_uint - Writes an unsigned integer to a memory buffer.
#cat:
#cat: flush_bits - Writes remaining bits to a memory buffer.
#cat:

***********************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dataio.h>
#include <ioutil.h>
#include <swap.h>

/*******************************************************/
/* Read a byte of data from the input buffer.          */
/*******************************************************/
int getc_byte(
   unsigned char *ochar_dat,  /* pointer to returned byte       */
   unsigned char **cbufptr,   /* pointer to next byte in buffer */
   unsigned char *ebufptr)    /* pointer to end of buffer       */
{
   //unsigned char char_dat;

   /* If at end of buffer ... */
   if((*cbufptr) >= ebufptr){
      fprintf(stderr, "ERROR : getc_byte : premature End Of Buffer\n");
      return(-39);
   }

   /* Assign next byte in buffer. */
   *ochar_dat = **cbufptr;
   /* Bump buffer pointer. */
   (*cbufptr)++;

   return(0);
}

/*******************************************************/
/* Read a sequence of data bytes from the input buffer.*/
/*******************************************************/
int getc_bytes(
   unsigned char **ochar_dat, /* pointer to returned bytes      */
   const int ilen,            /* number of bytes to be returned */
   unsigned char **cbufptr,   /* pointer to next byte in buffer */
   unsigned char *ebufptr)    /* pointer to end of buffer       */
{
   //unsigned char *char_dat;

   /* If at end of buffer ... */
   if((*cbufptr) >= (ebufptr-(ilen-1))){
      fprintf(stderr, "ERROR : getc_bytes : premature End Of Buffer\n");
      return(-40);
   }

   /* Copy sequence of bytes from buffer. */
   memcpy(*ochar_dat, *cbufptr, ilen);

   /* Bump buffer pointer. */
   (*cbufptr)+=ilen;

   return(0);
}

/***********************************************/
/* Stores a byte of data to the output buffer. */
/***********************************************/
int putc_byte(
   const unsigned char idata, /* input byte */
   unsigned char *odata,      /* output buffer of bytes */
   const int oalloc,          /* allocated size of output buffer */
   int *olen)                 /* filled length of output buffer  */
{
   /* olen points to next position in odata */
   /* If output buffer is out of space ...  */
   if((*olen) >= oalloc){
      fprintf(stderr,
      "ERROR : putc_byte : buffer overlow : alloc = %d, request = %d\n",
      oalloc, *olen);
      return(-32);
   }

   *(odata+(*olen)) = idata;
   (*olen)++;

   return(0);
}

/**********************************************************************/
/* Stores a vector of bytes of specified length to the output buffer. */
/**********************************************************************/
int putc_bytes(
   unsigned char *idata,  /* input buffer of bytes           */
   const int ilen,        /* bytes to be copied              */
   unsigned char *odata,  /* output buffer of bytes          */
   const int oalloc,      /* allocated size of output buffer */
   int *olen)             /* filled length of output buffer  */
{
   /* olen points to next position in odata */
   /* If output buffer is out of space ...  */
   if(((*olen)+ilen) > oalloc){
      fprintf(stderr,
      "ERROR : putc_bytes : buffer overlow : alloc = %d, request = %d\n",
      oalloc, (*olen)+ilen);
      return(-33);
   }

   memcpy(odata+(*olen), idata, ilen);
   (*olen) += ilen;

   return(0);
}

/************************************************************/
/* Routine to read an unsigned short from the input buffer. */
/************************************************************/
int getc_ushort(
   unsigned short *oshrt_dat,  /* pointer to returned unsigned short */
   unsigned char  **cbufptr,   /* pointer to next byte in buffer     */
   unsigned char  *ebufptr)    /* pointer to end of buffer           */
{
   int ret;
   unsigned short shrt_dat;
   unsigned char  *cptr;

   cptr = (unsigned char *)(&shrt_dat);

   ret = getc_bytes(&cptr, sizeof(unsigned short), cbufptr, ebufptr);
   if(ret)
      return(ret);

#if defined (__i386__) || defined(__x86_64__) || defined(_WIN32)
   swap_short_bytes(shrt_dat);
#endif

   *oshrt_dat = shrt_dat;
   return(0);
}

/***************************************************************/
/* This routine stores an unsigned short to the output buffer. */
/***************************************************************/
int putc_ushort(
   unsigned short ishort,     /* input unsigned short     */
   unsigned char *odata,      /* output byte buffer       */
   const int oalloc,          /* allocated size of buffer */
   int *olen)                 /* filled length of buffer  */
{
   int ret;
   unsigned char *cptr;

#if defined(__i386__) || defined(__x86_64__) || defined(_WIN32)
   swap_short_bytes(ishort);
#endif

   cptr = (unsigned char *)(&ishort);

   ret = putc_bytes(cptr, sizeof(unsigned short), odata, oalloc, olen);
   if(ret)
      return(ret);

   return(0);
}

/**********************************************************/
/* Routine to read an unsigned int from the input buffer. */
/**********************************************************/
int getc_uint(
   unsigned int  *oint_dat,  /* pointer to returned unsigned int */
   unsigned char **cbufptr,  /* pointer to next byte in buffer   */
   unsigned char *ebufptr)   /* pointer to end of buffer         */
{
   int ret;
   unsigned int int_dat;
   unsigned char  *cptr;

   cptr = (unsigned char *)(&int_dat);
   ret = getc_bytes(&cptr, sizeof(unsigned int), cbufptr, ebufptr);
   if(ret)
      return(ret);

#if defined(__i386__) || defined(__x86_64__) || defined(_WIN32)
   swap_int_bytes(int_dat);
#endif

   *oint_dat = int_dat;
   return(0);
}

/****************************************************/
/* Stores an unsigned integer to the output buffer. */
/****************************************************/
int putc_uint(
   unsigned int iint,        /* input unsigned int       */
   unsigned char *odata,     /* output byte buffer       */
   const int oalloc,         /* allocated size of buffer */
   int *olen)                /* filled length of buffer  */
{
   int ret;
   unsigned char *cptr;

#if defined(__i386__) || defined(__x86_64__) || defined(_WIN32)
   swap_int_bytes(iint);
#endif

   cptr = (unsigned char *)(&iint);

   ret = putc_bytes(cptr, sizeof(unsigned int), odata, oalloc, olen);
   if(ret)
      return(ret);

   return(0);
}

/*******************************************************/
/*Routine to write "compressed" bits to output buffer. */
/*******************************************************/
void write_bits(
   unsigned char **outbuf,    /* output data buffer                          */
   const unsigned short code, /* info to write into buffer                   */
   const short size,          /* numbers bits of code to write into buffer   */
   int *outbit,               /* current bit location in out buffer byte     */
   unsigned char *bits,       /* byte to write to output buffer              */
   int *bytes)                /* count of number bytes written to the buffer */
{
   short num;

   num = size;

   for(--num; num >= 0; num--) {
      *bits <<= 1;
      *bits |= ((unsigned char)((code >> num) & 0x0001));

      if(--(*outbit) < 0) {
         **outbuf = *bits;
         (*outbuf)++;
         if(*bits == 0xFF) {
            **outbuf = 0;
            (*outbuf)++;
            (*bytes)++;
         }
         (*bytes)++;
         *outbit = 7;
         *bits = 0;
      }
   }
   return;
}

/*********************************************/
/* Routine to "flush" left over bits in last */
/* byte after compressing a block.           */
/*********************************************/
void flush_bits(
   unsigned char **outbuf, /* output data buffer */
   int *outbit,            /* current bit location in out buffer byte */
   unsigned char *bits,    /* byte to write to output buffer */
   int *bytes)             /* count of number bytes written to the buffer */
{
   int cnt;              /* temp counter */

   if(*outbit != 7) {
      for(cnt = *outbit; cnt >= 0; cnt--) {
         *bits <<= 1;
         *bits |= 0x01;
      }

      **outbuf = *bits;
      (*outbuf)++;
      if(*bits == 0xFF) {
         *bits = 0;
         **outbuf = 0;
         (*outbuf)++;
         (*bytes)++;
      }
      (*bytes)++;
      *outbit = 7;
      *bits = 0;
   }
   return;
}
