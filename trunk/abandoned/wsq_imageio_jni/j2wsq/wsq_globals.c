/***********************************************************************
      LIBRARY: WSQ - Grayscale Image Compression

      FILE:    GLOBALS.C
      AUTHORS: Craig Watson
               Michael Garris
      DATE:    11/24/1999

      Contains global variable declarations and assignments
      that support WSQ image compression.

***********************************************************************/

#include <wsq.h>

/*
int debug;
*/

QUANT_VALS quant_vals;

W_TREE w_tree[W_TREELEN];

Q_TREE q_tree[Q_TREELEN];

DTT_TABLE dtt_table;

DQT_TABLE dqt_table;

DHT_TABLE dht_table[MAX_DHT_TABLES];

FRM_HEADER_WSQ frm_header_wsq;


#ifdef FILTBANK_EVEN_8X8_1
float hifilt[MAX_HIFILT] =  {
                              0.03226944131446922,
                             -0.05261415011924844,
                             -0.18870142780632693,
                              0.60328894481393847,
                             -0.60328894481393847,
                              0.18870142780632693,
                              0.05261415011924844,
                             -0.03226944131446922 };

float lofilt[MAX_LOFILT] =  {
                              0.07565691101399093,
                             -0.12335584105275092,
                             -0.09789296778409587,
                              0.85269867900940344,
                              0.85269867900940344,
                             -0.09789296778409587,
                             -0.12335584105275092,
                              0.07565691101399093 };
#else
float hifilt[MAX_HIFILT] =  { 0.06453888262893845,
                              -0.04068941760955844,
                              -0.41809227322221221,
                               0.78848561640566439,
                              -0.41809227322221221,
                              -0.04068941760955844,
                               0.06453888262893845 };

float lofilt[MAX_LOFILT] =  { 0.03782845550699546,
                              -0.02384946501938000,
                              -0.11062440441842342,
                               0.37740285561265380,
                               0.85269867900940344,
                               0.37740285561265380,
                              -0.11062440441842342,
                              -0.02384946501938000,
                               0.03782845550699546 };
#endif
