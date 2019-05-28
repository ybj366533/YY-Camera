#include "YY_png.h"


YYPng::YYPng( ) {
	// TODO Auto-generated constructor stub
	//m_pAssetManager = pAssetManager;
}

YYPng::~YYPng() {
	// TODO Auto-generated destructor stub
}



void YYPng::Png_read_data(png_structrp png_ptr, png_bytep data, png_size_t length)
{
	YYPng* pPng = ((YYPng*) png_get_io_ptr(png_ptr));

	if (pPng->m_nOffset + length > pPng->m_nSize)
		length = pPng->m_nSize - pPng->m_nOffset;

	memcpy (data, (const char*)pPng->m_pFileData + pPng->m_nOffset, length);
	pPng->m_nOffset += length;
}

int YYPng::getImageFromMemory(RGBQUAD ** ppdwDst, int* pnDstW,
		int* pnDstH, const void* pSrc, int nSize) {

	png_structp png_ptr = NULL;
	png_infop info_ptr = NULL;
	FILE* fp = NULL;
	png_byte* pbImageData = NULL;
	png_byte **ppbRowPointers = NULL;
    png_uint_32 width = 0, height = 0;
    int bit_depth = 0, color_type = 0;

	try {
		if (png_sig_cmp((png_const_bytep)pSrc, 0, 8)) {
			csTrace (L0, "fkPng::getImageFromMemory png_sig_cmp failed, format is invalid.\n");
			throw	-1;
		}

		if (NULL == (png_ptr = png_create_read_struct(PNG_LIBPNG_VER_STRING, NULL, NULL, NULL))) {
			csTrace(L0, "error: fkPng::getImageFromMemory png_create_read_struct failed.\n");
			throw -2;
		}

		if (NULL == (info_ptr = png_create_info_struct(png_ptr))) {
			csTrace(L0, "error: fkPng::getImageFromMemory png_create_info_struct failed.\n");
			throw -3;
		}

		m_nOffset	= 8;
		m_nSize		= nSize;
		m_pFileData	= pSrc;

        png_set_read_fn(png_ptr, this, Png_read_data);

        png_set_sig_bytes(png_ptr, 8);

        png_read_info(png_ptr, info_ptr);

        png_get_IHDR(png_ptr, info_ptr, &width, &height, &bit_depth, &color_type, NULL, NULL, NULL);

        if (color_type == PNG_COLOR_TYPE_PALETTE)
        	png_set_expand(png_ptr);
		if (bit_depth < 8)
			png_set_expand(png_ptr);
		if (png_get_valid(png_ptr, info_ptr, PNG_INFO_tRNS))
			png_set_expand(png_ptr);
		if (color_type == PNG_COLOR_TYPE_GRAY || color_type == PNG_COLOR_TYPE_GRAY_ALPHA)
			png_set_gray_to_rgb(png_ptr);

		png_read_update_info(png_ptr, info_ptr);
		png_get_IHDR(png_ptr, info_ptr, &width, &height, &bit_depth, &color_type, NULL, NULL, NULL);

		png_uint_32 ulRowBytes = png_get_rowbytes(png_ptr, info_ptr);
		png_uint_32 ulChannels = png_get_channels(png_ptr, info_ptr);

		if ((pbImageData = (png_byte *) malloc(ulRowBytes * height * sizeof(png_byte))) == NULL) {
			csTrace(L0, "error: fkPng::getImageFromMemory PNG: pbImageData out of memory.\n");
			throw -4;
		}
		if ((ppbRowPointers = (png_bytepp) malloc(height * sizeof(png_bytep))) == NULL) {
			csTrace(L0, "error: fkPng::getImageFromMemory PNG: ppbRowPointers out of memory.\n");
			throw -5;
		}
		for (int i = 0; i < height; i++)
			ppbRowPointers[i] = pbImageData + i * ulRowBytes;

		png_read_image(png_ptr, ppbRowPointers);

		png_read_end(png_ptr, NULL);

		throw 1;
	} catch (int nRet) {

		if (png_ptr) {
			png_destroy_read_struct(&png_ptr, &info_ptr, NULL);
		}

		FREE (ppbRowPointers);

		FCLOSE(fp);

		if (1 == nRet) {
			*pnDstW	= width;
			*pnDstH = height;
			int nArea = width * height;
			switch (color_type)
			{
			case PNG_COLOR_TYPE_RGB:
				{
					RGBQUAD* pRgba = (RGBQUAD*)malloc(nArea * sizeof(RGBQUAD));
					memset (pRgba, 0, nArea * sizeof(RGBQUAD));
					RGBTRIPLE* pRgb = (RGBTRIPLE*)pbImageData;
					for (int nIdx = 0; nIdx < nArea; nIdx++)
					{
						pRgba[nIdx].rgbRed = pRgb[nIdx].rgbRed;
						pRgba[nIdx].rgbGreen = pRgb[nIdx].rgbGreen;
						pRgba[nIdx].rgbBlue = pRgb[nIdx].rgbBlue;
						pRgba[nIdx].rgbReserved = 0xff;
					}
					FREE (pbImageData);
					*ppdwDst = pRgba;
				}
				break;
			case PNG_COLOR_TYPE_RGBA:
				{
					*ppdwDst = (RGBQUAD*)pbImageData;
				}
				break;
			case PNG_COLOR_TYPE_PALETTE:
			case PNG_COLOR_TYPE_GRAY:
			case PNG_COLOR_TYPE_GA:
			default:
				{
					FREE (pbImageData);
					csTrace(L0, "error: getImageFromMemory failed, cannot support this format yeat. color_type=[%d].\n", color_type);
					return	-7;
				}
				break;
			}
		}
		return	nRet;
	}

	return	0;
}

int YYPng::getImageFromStdio(RGBQUAD ** ppdwImg, int* pnImgW, int* pnImgH,
		FILE* fp) {
	if (NULL == fp) {
		csTrace(L0, "error: fkPng::getImageFromStdio failed, fp=NULL\n");
		return	-1;
	}

	fseek (fp, 0, SEEK_END);
	int nSize = ftell (fp);
	fseek (fp, 0, SEEK_SET);
	void* pSrc = malloc(nSize);
	fread (pSrc, 1, nSize, fp);
	int nRet = getImageFromMemory(ppdwImg, pnImgW, pnImgH, pSrc, nSize);
	FREE (pSrc);

	return	nRet;
}

int YYPng::getImageFromFile(RGBQUAD ** ppdwImg, int* pnImgW, int* pnImgH,
		const char* szFile) {
	FILE* fp = fopen (szFile, "rb");
	if (NULL == fp) {
		csTrace(L1, "waring: fkPng::getImageFromFile fopen failed, file=[%s] errinfo=[%s]\n",
				szFile, ERRINFO);
		return	-1;
	}

	int nRet = getImageFromStdio(ppdwImg, pnImgW, pnImgH, fp);

	FCLOSE(fp);

	return	nRet;
}

