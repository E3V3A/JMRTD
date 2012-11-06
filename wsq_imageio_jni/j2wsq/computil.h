#ifndef _COMPUTIL_H
#define _COMPUTIL_H

extern int read_skip_marker_segment(const unsigned short, FILE *);
extern int getc_skip_marker_segment(const unsigned short,
                            unsigned char **, unsigned char *);

#endif /* !_COMPUTIL_H */

