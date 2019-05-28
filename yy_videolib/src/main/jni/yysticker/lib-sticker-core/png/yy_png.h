#ifndef YY_png_hpp
#define YY_png_hpp
#include <errno.h>
#include <string.h>
//#include <android/asset_manager.h>
#if defined(TARGET_OS_ANDROID)
#include <android/log.h>
#endif
#include "png.h"
#include "pngpriv.h"

#define 	FREE(p)		if(p) free(p); p=NULL;
#define 	FCLOSE(fp)	if(fp) fclose(fp); fp=NULL;
#define 	ERRINFO		strerror(errno)
#if defined(TARGET_OS_ANDROID)
#define 	csTrace(level, ...)		__android_log_print(ANDROID_LOG_ERROR, "fuke", __VA_ARGS__)
#else
#define     csTrace(level, ...)     printf(__VA_ARGS__)
#endif
#define 	L0	0
#define		L1	1
#define		L2	2
#define		L3	3

#pragma pack(1)

typedef struct tagRGBQUAD {
	unsigned char   rgbRed;
	unsigned char   rgbGreen;
	unsigned char   rgbBlue;
	unsigned char	rgbReserved;
} RGBQUAD;

typedef struct tagRGBTRIPLE {
	unsigned char    rgbRed;
	unsigned char    rgbGreen;
	unsigned char    rgbBlue;
} RGBTRIPLE;

#pragma pack()


class YYPng {
public:
	YYPng();
	virtual ~YYPng();

	int getImageFromMemory(RGBQUAD** ppdwDst, int* pnDstW, int* pnDstH,
			const void* pSrc, int nSize);

	int getImageFromStdio(RGBQUAD ** ppdwImg, int* pnImgW, int* pnImgH,
			FILE* fp);

	int getImageFromFile(RGBQUAD ** ppdwImg, int* pnImgW, int* pnImgH,
			const char* szFile);


private:
	static void Png_read_data(png_structrp png_ptr, png_bytep data, png_size_t length);

	int 	m_nOffset;
	int 	m_nSize;
	const void*	m_pFileData;
};

#endif

