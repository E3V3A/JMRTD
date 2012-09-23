/***********************************************************************
      LIBRARY: WSQ - Grayscale Image Compression

      FILE:    DECODER.C
      AUTHORS: Craig Watson
               Michael Garris
      DATE:    12/02/1999

      Contains routines responsible for decoding a WSQ compressed
      datastream.

      ROUTINES:
#cat: wsq_decode_mem - Decodes a datastream of WSQ compressed bytes
#cat:                  from a memory buffer, returning a lossy
#cat:                  reconstructed pixmap.
#cat: huffman_decode_data_mem - Decodes a block of huffman encoded
#cat:                  data from a memory buffer.
#cat: huffman_decode_data_file - Decodes a block of huffman encoded
#cat:                  data from an open file.
#cat: decode_data_mem - Decodes huffman encoded data from a memory buffer.
#cat:
#cat: nextbits_wsq - Gets next sequence of bits for data decoding from
#cat:                    an open file.
#cat: getc_nextbits_wsq - Gets next sequence of bits for data decoding
#cat:                    from a memory buffer.

***********************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <wsq.h>
#include <dataio.h>

/************************************************************************/
/*              This is an implementation based on the Crinimal         */
/*              Justice Information Services (CJIS) document            */
/*              "WSQ Gray-scale Fingerprint Compression                 */
/*              Specification", Dec. 1997.                              */
/***************************************************************************/
/* WSQ Decoder routine.  Takes an WSQ compressed memory buffer and decodes */
/* it, returning the reconstructed pixmap.                                 */
/***************************************************************************/
int wsq_decode_mem(unsigned char **odata, int *ow, int *oh, int *od, int *oppi,
                   int *lossyflag, unsigned char *idata, const int ilen)
{
   int ret, i;
   unsigned short marker;         /* WSQ marker */
   int num_pix;                   /* image size and counter */
   int width, height, ppi;        /* image parameters */
   unsigned char *cdata;          /* image pointer */
   float *fdata;                  /* image pointers */
   short *qdata;                  /* image pointers */
   unsigned char *cbufptr;        /* points to current byte in buffer */
   unsigned char *ebufptr;        /* points to end of buffer */

   /* Set memory buffer pointers. */
   cbufptr = idata;
   ebufptr = idata + ilen;

   /* Init DHT Tables to 0. */
   for(i = 0; i < MAX_DHT_TABLES; i++)
      (dht_table + i)->tabdef = 0;

   /* Read the SOI marker. */
   ret = getc_marker_wsq(&marker, SOI_WSQ, &cbufptr, ebufptr);
   if(ret){
      return(ret);
   }

   /* Read in supporting tables up to the SOF marker. */
   ret = getc_marker_wsq(&marker, TBLS_N_SOF, &cbufptr, ebufptr);
   if(ret){
      return(ret);
   }
   while(marker != SOF_WSQ) {
      ret = getc_table_wsq(marker, &dtt_table, &dqt_table, dht_table,
		      &cbufptr, ebufptr);
      if(ret){
         return(ret);
      }
      ret = getc_marker_wsq(&marker, TBLS_N_SOF, &cbufptr, ebufptr);
      if(ret){
         return(ret);
      }
   }

   /* Read in the Frame Header. */
   ret = getc_frame_header_wsq(&frm_header_wsq, &cbufptr, ebufptr);
   if(ret){
      return(ret);
   }
   width = frm_header_wsq.width;
   height = frm_header_wsq.height;
   num_pix = width * height;

   ret = getc_ppi_wsq(&ppi, idata, ilen);
   if(ret)
      return(ret);

   if(debug > 0)
      fprintf(stderr, "SOI, tables, and frame header read\n\n");

   /* Build WSQ decomposition trees. */
   build_wsq_trees(w_tree, W_TREELEN, q_tree, Q_TREELEN, width, height);

   if(debug > 0)
      fprintf(stderr, "Tables for wavelet decomposition finished\n\n");

   /* Allocate working memory. */
   qdata = (short *) malloc(num_pix * sizeof(short));
   if(qdata == (short *)NULL) {
      fprintf(stderr,"ERROR: wsq_decode_mem : malloc : qdata1\n");
      return(-20);
   }

   /* Decode the Huffman encoded data blocks. */
   ret = huffman_decode_data_mem(qdata, &dtt_table, &dqt_table,
		   dht_table, &cbufptr, ebufptr);
   if(ret){
      free(qdata);
      return(ret);
   }

   if(debug > 0)
      fprintf(stderr,
         "Quantized WSQ subband data blocks read and Huffman decoded\n\n");

   /* Decode the quantize wavelet subband data. */
   ret = unquantize(&fdata, &dqt_table, q_tree, Q_TREELEN,
		   qdata, width, height);
   if(ret){
      free(qdata);
      return(ret);
   }

   if(debug > 0)
      fprintf(stderr, "WSQ subband data blocks unquantized\n\n");

   /* Done with quantized wavelet subband data. */
   free(qdata);

   ret = wsq_reconstruct(fdata, width, height, w_tree, W_TREELEN, &dtt_table);
   if(ret){
      free(fdata);
      return(ret);
   }

   if(debug > 0)
      fprintf(stderr, "WSQ reconstruction of image finished\n\n");

   cdata = (unsigned char *)malloc(num_pix * sizeof(unsigned char));
   if(cdata == (unsigned char *)NULL) {
      free(fdata);
      fprintf(stderr,"ERROR: wsq_decode_mem : malloc : cdata\n");
      return(-21);
   }

   /* Convert floating point pixels to unsigned char pixels. */
   conv_img_2_uchar(cdata, fdata, width, height,
                      frm_header_wsq.m_shift, frm_header_wsq.r_scale);

   /* Done with floating point pixels. */
   free(fdata);

   if(debug > 0)
      fprintf(stderr, "Doubleing point pixels converted to unsigned char\n\n");


   /* Assign reconstructed pixmap and attributes to output pointers. */
   *odata = cdata;
   *ow = width;
   *oh = height;
   *od = 8;
   *oppi = ppi;
   *lossyflag = 1;

   /* Return normally. */
   return(0);
}

/***************************************************************************/
/* Routine to decode an entire "block" of encoded data from memory buffer. */
/***************************************************************************/
int huffman_decode_data_mem(
   short *ip,               /* image pointer */
   DTT_TABLE *dtt_table,    /*transform table pointer */
   DQT_TABLE *dqt_table,    /* quantization table */
   DHT_TABLE *dht_table,    /* huffman table */
   unsigned char **cbufptr, /* points to current byte in input buffer */
   unsigned char *ebufptr)  /* points to end of input buffer */
{
   int ret;
   int blk = 0;           /* block number */
   unsigned short marker; /* WSQ markers */
   int bit_count;         /* bit count for getc_nextbits_wsq routine */
   int n;                 /* zero run count */
   int nodeptr;           /* pointers for decoding */
   int last_size;         /* last huffvalue */
   unsigned char hufftable_id;    /* huffman table number */
   HUFFCODE *hufftable;   /* huffman code structure */
   int maxcode[MAX_HUFFBITS+1]; /* used in decoding data */
   int mincode[MAX_HUFFBITS+1]; /* used in decoding data */
   int valptr[MAX_HUFFBITS+1];     /* used in decoding data */
   unsigned short tbits;


   ret = getc_marker_wsq(&marker, TBLS_N_SOB, cbufptr, ebufptr);
   if(ret)
      return(ret);

   bit_count = 0;

   while(marker != EOI_WSQ) {

      if(marker != 0) {
         blk++;
         while(marker != SOB_WSQ) {
            ret = getc_table_wsq(marker, dtt_table, dqt_table,
			    dht_table, cbufptr, ebufptr);
            if(ret)
               return(ret);
	    ret = getc_marker_wsq(&marker, TBLS_N_SOB, cbufptr, ebufptr);
            if(ret)
               return(ret);
         }
	 ret = getc_block_header(&hufftable_id, cbufptr, ebufptr);
         if(ret)
            return(ret);

         if((dht_table+hufftable_id)->tabdef != 1) {
            fprintf(stderr, "ERROR : huffman_decode_data_mem : ");
            fprintf(stderr, "huffman table {%d} undefined.\n", hufftable_id);
            return(-51);
         }

         /* the next two routines reconstruct the huffman tables */
	 ret = build_huffsizes(&hufftable, &last_size,
			 (dht_table+hufftable_id)->huffbits,
			 MAX_HUFFCOUNTS_WSQ);
         if(ret)
            return(ret);

         build_huffcodes(hufftable);
	 ret = check_huffcodes_wsq(hufftable, last_size);
         if(ret)
            fprintf(stderr, "         hufftable_id = %d\n", hufftable_id);

         /* this routine builds a set of three tables used in decoding */
         /* the compressed data*/
         gen_decode_table(hufftable, maxcode, mincode, valptr,
                          (dht_table+hufftable_id)->huffbits);
         free(hufftable);
         bit_count = 0;
         marker = 0;
      }

      /* get next huffman category code from compressed input data stream */
      ret = decode_data_mem(&nodeptr, mincode, maxcode, valptr,
		      (dht_table+hufftable_id)->huffvalues, cbufptr,
		      ebufptr, &bit_count, &marker);
      if(ret)
         return(ret);

      if(nodeptr == -1)
         continue;

      if(nodeptr > 0 && nodeptr <= 100)
         for(n = 0; n < nodeptr; n++) {
            *ip++ = 0; /* z run */
         }
      else if(nodeptr > 106 && nodeptr < 0xff)
         *ip++ = nodeptr - 180;
      else if(nodeptr == 101){
         ret = getc_nextbits_wsq(&tbits, &marker, cbufptr,
			 ebufptr, &bit_count, 8);
         if(ret)
            return(ret);
         *ip++ = tbits;
      }
      else if(nodeptr == 102){
         ret = getc_nextbits_wsq(&tbits, &marker, cbufptr,
			 ebufptr, &bit_count, 8);
         if(ret)
            return(ret);
         *ip++ = -tbits;
      }
      else if(nodeptr == 103){
         ret = getc_nextbits_wsq(&tbits, &marker, cbufptr,
			 ebufptr, &bit_count, 16);
         if(ret)
            return(ret);
         *ip++ = tbits;
      }
      else if(nodeptr == 104){
         ret = getc_nextbits_wsq(&tbits, &marker, cbufptr,
			 ebufptr, &bit_count, 16);
         if(ret)
            return(ret);
         *ip++ = -tbits;
      }
      else if(nodeptr == 105) {
         ret = getc_nextbits_wsq(&tbits, &marker, cbufptr,
			 ebufptr, &bit_count, 8);
         if(ret)
            return(ret);
         n = tbits;
         while(n--)
            *ip++ = 0;
      }
      else if(nodeptr == 106) {
         ret = getc_nextbits_wsq(&tbits, &marker, cbufptr,
			 ebufptr, &bit_count, 16);
         if(ret)
            return(ret);
         n = tbits;
         while(n--)
            *ip++ = 0;
      }
      else {
         fprintf(stderr, 
                "ERROR: huffman_decode_data_mem : Invalid code %d (%x).\n",
                nodeptr, nodeptr);
         return(-52);
      }

   }

   return(0);
}

/**********************************************************/
/* Routine to decode the encoded data from memory buffer. */
/**********************************************************/
int decode_data_mem(
   int *onodeptr,       /* returned huffman code category        */
   int *mincode,        /* points to minimum code value for      */
                        /*    a given code length                */
   int *maxcode,        /* points to maximum code value for      */
                        /*    a given code length                */
   int *valptr,         /* points to first code in the huffman   */
                        /*    code table for a given code length */
   unsigned char *huffvalues,   /* defines order of huffman code          */
                                /*    lengths in relation to code sizes   */
   unsigned char **cbufptr,     /* points to current byte in input buffer */
   unsigned char *ebufptr,      /* points to end of input buffer          */
   int *bit_count,      /* marks the bit to receive from the input byte */
   unsigned short *marker)
{
   int ret;
   int inx, inx2;       /*increment variables*/
   unsigned short code, tbits;  /* becomes a huffman code word
                                   (one bit at a time)*/

   ret = getc_nextbits_wsq(&code, marker, cbufptr, ebufptr, bit_count, 1);
   if(ret)
      return(ret);

   if(*marker != 0){
      *onodeptr = -1;
      return(0);
   }

   for(inx = 1; (int)code > maxcode[inx]; inx++) {
      ret = getc_nextbits_wsq(&tbits, marker, cbufptr, ebufptr, bit_count, 1);
      if(ret)
         return(ret);

      code = (code << 1) + tbits;
      if(*marker != 0){
         *onodeptr = -1;
         return(0);
      }
   }
   inx2 = valptr[inx];
   inx2 = inx2 + code - mincode[inx];

   *onodeptr = huffvalues[inx2];
   return(0);
}

/****************************************************************/
/* Routine to get nextbit(s) of data stream from memory buffer. */
/****************************************************************/
int getc_nextbits_wsq(
   unsigned short *obits,       /* returned bits */
   unsigned short *marker,      /* returned marker */
   unsigned char **cbufptr,     /* points to current byte in input buffer */
   unsigned char *ebufptr,      /* points to end of input buffer */
   int *bit_count,      /* marks the bit to receive from the input byte */
   const int bits_req)  /* number of bits requested */
{
   int ret;
   static unsigned char code;   /*next byte of data*/
   static unsigned char code2;  /*stuffed byte of data*/
   unsigned short bits, tbits;  /*bits of current data byte requested*/
   int bits_needed;     /*additional bits required to finish request*/

                              /*used to "mask out" n number of
                                bits from data stream*/
   static unsigned char bit_mask[9] = {0x00,0x01,0x03,0x07,0x0f,
                                       0x1f,0x3f,0x7f,0xff};
   if(*bit_count == 0) {
      ret = getc_byte(&code, cbufptr, ebufptr);
      if(ret){
         return(ret);
      }
      *bit_count = 8;
      if(code == 0xFF) {
         ret = getc_byte(&code2, cbufptr, ebufptr);
         if(ret){
            return(ret);
         }
         if(code2 != 0x00 && bits_req == 1) {
            *marker = (code << 8) | code2;
            *obits = 1;
            return(0);
         }
         if(code2 != 0x00) {
            fprintf(stderr, "ERROR: getc_nextbits_wsq : No stuffed zeros\n");
            return(-41);
         }
      }
   }
   if(bits_req <= *bit_count) {
      bits = (code >>(*bit_count - bits_req)) & (bit_mask[bits_req]);
      *bit_count -= bits_req;
      code &= bit_mask[*bit_count];
   }
   else {
      bits_needed = bits_req - *bit_count;
      bits = code << bits_needed;
      *bit_count = 0;
      ret = getc_nextbits_wsq(&tbits, (unsigned short *)NULL,
		      cbufptr, ebufptr, bit_count, bits_needed);
      if(ret)
         return(ret);
      bits |= tbits;
   }

   *obits = bits;
   return(0);
}
