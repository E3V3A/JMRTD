#ifndef _DATA_IO_H
#define _DATA_IO_H

/* dataio.c */
extern int getc_byte(unsigned char *, unsigned char **, unsigned char *);
extern int getc_bytes(unsigned char **, const int, unsigned char **, 
                 unsigned char *);
extern int putc_byte(const unsigned char, unsigned char *, const int, int *);
extern int putc_bytes(unsigned char *, const int, unsigned char *,
                 const int, int *);
extern int getc_ushort(unsigned short *, unsigned char **, unsigned char *);
extern int putc_ushort(unsigned short, unsigned char *, const int, int *);
extern int getc_uint(unsigned int *, unsigned char **, unsigned char *);
extern int putc_uint(unsigned int, unsigned char *, const int, int *);
extern void write_bits(unsigned char **, const unsigned short, const short,
                 int *, unsigned char *, int *);
extern void flush_bits(unsigned char **, int *, unsigned char *, int *);

#endif /* !_DATA_IO_H */
