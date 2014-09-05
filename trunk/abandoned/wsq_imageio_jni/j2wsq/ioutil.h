#ifndef True
#define True	1
#define False	0
#endif

#define MaxLineLength	512
#define EOL	EOF

/* fileexst.c */
extern char *fileext(void);
/* filehead.c */
extern void filehead(char *);
/* fileroot.c */
extern void fileroot(char *);
/* filesize.c */
extern int filesize(char *);
/* filetail.c */
extern void filetail(char *);
/* findfile.c */
extern int find_file(char *,char *);
extern int file_exists(char *);
