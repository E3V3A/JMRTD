#ifndef _WSQ_H
#define _WSQ_H

#ifndef _STDIO_H
#include <stdio.h>
#endif

#ifndef TRUE
#define TRUE  1
#define FALSE 0
#endif

#include "nistcom.h"

/* WSQ Marker Definitions */
#define SOI_WSQ 0xffa0
#define EOI_WSQ 0xffa1
#define SOF_WSQ 0xffa2
#define SOB_WSQ 0xffa3
#define DTT_WSQ 0xffa4
#define DQT_WSQ 0xffa5
#define DHT_WSQ 0xffa6
#define DRT_WSQ 0xffa7
#define COM_WSQ 0xffa8
/* Case for getting ANY marker. */
#define ANY_WSQ 0xffff
#define TBLS_N_SOF 2 /* MO, was in jpegl.h */
#define TBLS_N_SOB   (TBLS_N_SOF + 2)

#define   READ_TABLE_LEN    1 /* MO, was in jpegl.h */
#define   NO_READ_TABLE_LEN 0 /* MO, was in jpegl.h */


/* Filter Bank Definitions */
#ifdef FILTBANK_EVEN_8X8_1
#define MAX_HIFILT   8
#define MAX_LOFILT   8
#else
#define MAX_HIFILT   7
#define MAX_LOFILT   9
#endif

/* Subband Definitions */
#define STRT_SUBBAND_2      19
#define STRT_SUBBAND_3      52
#define MAX_SUBBANDS        64
#define NUM_SUBBANDS        60
#define STRT_SUBBAND_DEL    (NUM_SUBBANDS)
#define STRT_SIZE_REGION_2  4
#define STRT_SIZE_REGION_3  51

#define MIN_IMG_DIM         256

#define WHITE               255
#define BLACK               0

#define COEFF_CODE          0
#define RUN_CODE            1

#define RAW_IMAGE           1
#define IHEAD_IMAGE         0

#define VARIANCE_THRESH     1.01

typedef struct quantization {
   float q;  /* quantization level */
   float cr; /* compression ratio */
   float r;  /* compression bitrate */
   float qbss_t[MAX_SUBBANDS];
   float qbss[MAX_SUBBANDS];
   float qzbs[MAX_SUBBANDS];
   float var[MAX_SUBBANDS];
} QUANT_VALS;

typedef struct wavlet_tree {
   int x;
   int y;
   int lenx;
   int leny;
   int inv_rw;
   int inv_cl;
} W_TREE;
#define W_TREELEN 20

typedef struct quant_tree {
   short x;	/* UL corner of block */
   short y;
   short lenx;  /* block size */
   short leny;  /* block size */
} Q_TREE;
#define Q_TREELEN 64

typedef struct hcode {
   short  size;
   unsigned short code;
} HUFFCODE;

typedef struct table_dtt {
   float *lofilt;
   float *hifilt;
   unsigned char losz;
   unsigned char hisz;
   char lodef;
   char hidef;
} DTT_TABLE;

typedef struct table_dqt {
   float bin_center;
   float q_bin[MAX_SUBBANDS];
   float z_bin[MAX_SUBBANDS];
   char dqt_def;
} DQT_TABLE;

#define MAX_DHT_TABLES  8

#define MAX_HUFFBITS      16  	/* DO NOT CHANGE THIS CONSTANT!! */
#define MAX_HUFFCOUNTS_WSQ  256  /* Length of code table: change as needed */
                                 /* but DO NOT EXCEED 256 */
#define MAX_HUFFCOEFF        74  /* -73 .. +74 */
#define MAX_HUFFZRUN        100

typedef struct table_dht {
   unsigned char tabdef;
   unsigned char huffbits[MAX_HUFFBITS];
   unsigned char huffvalues[MAX_HUFFCOUNTS_WSQ+1];
} DHT_TABLE;

typedef struct header_frm {
   unsigned char black;
   unsigned char white;
   unsigned short width;
   unsigned short height;
   float m_shift; 
   float r_scale;
   unsigned char wsq_encoder;
   unsigned short software;
} FRM_HEADER_WSQ;

/* External global variables. */
extern int debug;
extern QUANT_VALS quant_vals;
extern W_TREE w_tree[];
extern Q_TREE q_tree[];
extern DTT_TABLE dtt_table;
extern DQT_TABLE dqt_table;
extern DHT_TABLE dht_table[];
extern FRM_HEADER_WSQ frm_header_wsq;
extern float hifilt[];
extern float lofilt[];


/* External function definitions. */

/* encoder.c */
extern int wsq_encode_mem(unsigned char **, int *, const float, unsigned char *,
                 const int, const int, const int, const int, char *);
extern int gen_hufftable_wsq(HUFFCODE **, unsigned char **, unsigned char **,
                 short *, const int *, const int);
extern int compress_block(unsigned char *, int *, short *,
                 const int, const int, const int, HUFFCODE *);
extern int count_block(int **, const int, short *,
                 const int, const int, const int);

/* decode.c */
extern int wsq_decode_mem(unsigned char **, int *, int *, int *, int *, int *,
                 unsigned char *, const int);
extern int huffman_decode_data_mem(short *, DTT_TABLE *, DQT_TABLE *,
                 DHT_TABLE *, unsigned char **, unsigned char *);
extern int decode_data_mem(int *, int *, int *, int *, unsigned char *,
                 unsigned char **, unsigned char *, int *, unsigned short *);
extern int getc_nextbits_wsq(unsigned short *, unsigned short *,
                 unsigned char **, unsigned char *, int *, const int);

/* huff.c */
extern int check_huffcodes_wsq(HUFFCODE *, int);

/* ppi.c */
extern int getc_ppi_wsq(int *, unsigned char *, const int);

/* tableio.c */
extern int getc_marker_wsq(unsigned short *, const int, unsigned char **,
                 unsigned char *);
extern int getc_table_wsq(unsigned short, DTT_TABLE *, DQT_TABLE *, DHT_TABLE *,
                 unsigned char **, unsigned char *);
extern int getc_transform_table(DTT_TABLE *, unsigned char **, unsigned char *);
extern int putc_transform_table(float *, const int, float *, const int,
                 unsigned char *, const int, int *);
extern int getc_quantization_table(DQT_TABLE *, unsigned char **,
                 unsigned char *);
extern int putc_quantization_table(QUANT_VALS *, unsigned char *, const int,
                 int *);
extern int getc_huffman_table_wsq(DHT_TABLE *, unsigned char **,
                 unsigned char *);
extern int getc_frame_header_wsq(FRM_HEADER_WSQ *, unsigned char **,
                 unsigned char *);
extern int putc_frame_header_wsq(const int, const int, const float,
                 const float, unsigned char *, const int, int *);
extern int getc_block_header(unsigned char *, unsigned char **,
                 unsigned char *);
extern int putc_block_header(const int, unsigned char *, const int, int *);
extern int add_comment_wsq(unsigned char **, int *, unsigned char *,
                 const int, unsigned char *);
extern int putc_nistcom_wsq(char *, const int, const int, const int,
                 const int, const int, const float, unsigned char *,
                 const int, int *);
extern int getc_nistcom_wsq(NISTCOM **, unsigned char *, const int);

/* tree.c */
extern void build_wsq_trees(W_TREE w_tree[], const int,
                 Q_TREE q_tree[], const int, const int, const int);
extern void build_w_tree(W_TREE w_tree[], const int, const int);
extern void w_tree4(W_TREE w_tree[], const int, const int,
                 const int, const int, const int, const int, const int);
extern void build_q_tree(W_TREE w_tree[], Q_TREE q_tree[]);
extern void q_tree16(Q_TREE q_tree[], const int, const int, const int,
                 const int, const int, const int, const int);
extern void q_tree4(Q_TREE q_tree[], const int, const int, const int,
                 const int, const int);

/* wsq_utils.c */
extern void conv_img_flt_new(float *, float *, float *,
                 unsigned char *, const int);
extern void conv_img_2_flt( float *fip, float *m_shift,
		float *r_scale, unsigned char *data, const int num_pix);
extern void conv_img_2_uchar(unsigned char *, float *, const int, const int,
                 const float, const float);
extern void variance( QUANT_VALS *quant_vals, Q_TREE q_tree[], const int,
                 float *, const int, const int);
extern int quantize(short **, int *, QUANT_VALS *, Q_TREE qtree[], const int,
                 float *, const int, const int);
extern void quant_block_sizes(int *, int *, int *,
                 QUANT_VALS *, W_TREE w_tree[], const int,
                 Q_TREE q_tree[], const int);
extern int unquantize(float **, const DQT_TABLE *,
                 Q_TREE q_tree[], const int, short *, const int, const int);
extern int wsq_decompose(float *, const int, const int,
                 W_TREE w_tree[], const int, float *, const int,
                 float *, const int);
extern void get_lets(float *, float *, const int, const int, const int,
                 const int, float *, const int, float *, const int, const int);
extern int wsq_reconstruct(float *, const int, const int,
                 W_TREE w_tree[], const int, const DTT_TABLE *);
extern void  join_lets(float *, float *, const int, const int,
                 const int, const int, float *, const int,
                 float *, const int, const int);
extern int int_sign(const int);
extern int image_size(const int, short *, short *);

/* MO, was in jpegl.h, defined in huff.c */
extern int getc_huffman_table(unsigned char *, unsigned char **,
                   unsigned char **, const int, unsigned char **,
                   unsigned char *, const int, int *);
extern int putc_huffman_table(const unsigned short, const unsigned char,
                   unsigned char *, unsigned char *, unsigned char *,
                   const int, int *);
extern int find_huff_sizes(int **, int *, const int);
extern void find_least_freq(int *, int *, int *, const int);
extern int find_num_huff_sizes(unsigned char **, int *, int *, const int);
extern int sort_huffbits(unsigned char *);
extern int sort_code_sizes(unsigned char **, int *, const int);
extern int build_huffcode_table(HUFFCODE **, HUFFCODE *, const int,
                   unsigned char *, const int);
extern int build_huffsizes(HUFFCODE **, int *, unsigned char *, const int);
extern void build_huffcodes(HUFFCODE *);
extern void gen_decode_table(HUFFCODE *, int *, int *, int *, unsigned char *);

#endif /* !_WSQ_H */
