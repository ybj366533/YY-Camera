
//todo
#define _CRT_SECURE_NO_WARNINGS

#include "yy_mp4Remux.h"

#include <stdint.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>


#define D_MOOV_BOX_MAX_SIZE			2 * 1024 *1024					//够不够？

#define D_FILE_READ_BUF_SIZE		512

// 这个不是严格意义的box的结构体的定义，并不反应 box的层级关系
// 只是定义了感兴趣的box在 moov_buf位置。
// # stss：标记关键帧的，这边只考虑所有帧都是关键帧的，将忽略这个box
typedef struct st_mp4_moov_box_t{
	uint8_t *moov_buf;		// 指向moov的开始位置
	uint8_t *stts_buf;		// 指向moov中的stts开始位置 Time to Sample Box, 每帧相对于前面一帧的时间差，如果时间差相同，可以合并成一项
	uint8_t *stsc_buf;		// 指向moov中的stsc开始位置 Sample to Chunk Box, 每个Chunk含有的帧数
	uint8_t *stsz_buf;		// 指向moov中的stsz开始位置 Sample Size Box 每帧的大小，每帧都是一个独立项
	uint8_t *stco_buf;		// 指向moov中的stco开始位置 Chunk Offset Box, 每个Chunk相对于文件开始的起始位置
} st_mp4_moov_box;

static inline uint32_t read_u32(const uint8_t * p)
{
	return (p[0] << 24) | (p[1] << 16) | (p[2] << 8) | p[3];
}

static inline uint32_t read_u32_from_file(FILE *fp)
{
	uint8_t p[4];
	int size = fread(p, 1, 4, fp);
	
	if (size != 4) {
		return 0;
	}

	return (p[0] << 24) | (p[1] << 16) | (p[2] << 8) | p[3];
}

static inline void write_u32(uint8_t *p, uint32_t value)
{
	p[0] = (uint8_t)((value >> 24) & 0xff);
	p[1] = (uint8_t)((value >> 16) & 0xff);
	p[2] = (uint8_t)((value >> 8) & 0xff);
	p[3] = (uint8_t)((value) & 0xff);
}

static inline uint64_t read_u64(const uint8_t * p)
{
	return  ((uint64_t)p[0] << 56) |
		((uint64_t)p[1] << 48) |
		((uint64_t)p[2] << 40) |
		((uint64_t)p[3] << 32) |
		((uint64_t)p[4] << 24) |
		((uint64_t)p[5] << 16) |
		((uint64_t)p[6] << 8) |
		(uint64_t)p[7];
}

static inline uint64_t read_u64_from_file(FILE *fp)
{
	uint8_t p[8];
	int size = fread(p, 1, 8, fp);

	if (size != 8) {
		return 0;
	}

	return  ((uint64_t)p[0] << 56) |
		((uint64_t)p[1] << 48) |
		((uint64_t)p[2] << 40) |
		((uint64_t)p[3] << 32) |
		((uint64_t)p[4] << 24) |
		((uint64_t)p[5] << 16) |
		((uint64_t)p[6] << 8) |
		(uint64_t)p[7];
}

static inline void get_box_type(const uint8_t * p, char* box_type)
{
	memcpy(box_type, p, 4);
}

static inline void write_box_type(uint8_t * p, char* box_type)
{
	memcpy(p, box_type, 4);
}

static inline void get_box_type_from_file(FILE *fp, char* box_type)
{
	uint8_t p[4];
	int size = fread(p, 1, 4, fp);

	if (size != 4) {
		memset(box_type, 0x00, 4);
		return;
	}
	memcpy(box_type, p, 4);
}

static uint32_t get_moov_box_pos(FILE *fp, uint32_t *moov_size)
{
	uint32_t pos = 0;
	char box_type[4] = { 0 };

	fseek(fp, 0, SEEK_SET);
	while (!feof(fp))
	{
		int head_size = 8; // 从box的开始位置算起

		uint64_t        box_len = read_u32_from_file(fp);

		// mdat_box会比较大，但短视频应该不会出现这个情况
		if (box_len == 1) {
			box_len = read_u64_from_file(fp);
			head_size += 8;
		}
		
		get_box_type_from_file(fp, box_type);

		if (memcmp(box_type, "moov", 4) == 0) {
			*moov_size = (uint32_t)box_len;
			pos = ftell(fp) - head_size;
			break;
		}
		else {
			// 非本次目的，直接跳过
			fseek(fp, box_len - head_size, SEEK_CUR);
		}
	}

	return pos;
}

// moov
// -- trak
// -- -- mdia
// -- -- -- minf
// -- -- -- -- stbl
// -- -- -- -- -- stts
// -- -- -- -- -- sttc
// -- -- -- -- -- stsz
// -- -- -- -- -- stco

int stbl_box_parse(uint8_t *minf, uint32_t box_size, st_mp4_moov_box *mp4_moov_box)
{

	char box_type[4] = { 0 };

	uint8_t *curr = minf + 8;
	const uint8_t *     end = minf + box_size;

	//目标：找到mdia box
	// 现今， 只考虑只有一个 视频box
	while (curr < end)
	{
		uint64_t        box_len = read_u32(curr);
		get_box_type(curr+4, box_type);
		//const uint8_t * box_data = curr + 8;

		if (memcmp(box_type, "stts", 4) == 0) {
			mp4_moov_box->stts_buf = curr;
			curr += box_len;
		}
		else if (memcmp(box_type, "stsc", 4) == 0) {
			mp4_moov_box->stsc_buf = curr;
			curr += box_len;
		}
		else if (memcmp(box_type, "stsz", 4) == 0) {
			mp4_moov_box->stsz_buf = curr;
			curr += box_len;
		}
		else if (memcmp(box_type, "stco", 4) == 0) {
			mp4_moov_box->stco_buf = curr;
			curr += box_len;
		}
		else {
			curr += box_len;
		}
	}
	return 0;
}

int minf_box_parse(uint8_t *minf, uint32_t box_size, st_mp4_moov_box *mp4_moov_box)
{

	char box_type[4] = { 0 };

	uint8_t *curr = minf + 8;
	const uint8_t *     end = minf + box_size;

	//目标：找到mdia box
	// 现今， 只考虑只有一个 视频box
	while (curr < end)
	{
		uint64_t        box_len = read_u32(curr);
		get_box_type(curr+4, box_type);

		if (memcmp(box_type, "stbl", 4) == 0) {
			stbl_box_parse(curr, box_len, mp4_moov_box);
			curr += box_len;
			break;
		}
		else {
			curr += box_len;
		}
	}
	return 0;
}

int mdia_box_parse(uint8_t *mdia, uint32_t box_size, st_mp4_moov_box *mp4_moov_box)
{

	char box_type[4] = { 0 };

	uint8_t *curr = mdia + 8;
	const uint8_t *     end = mdia + box_size;

	//目标：找到mdia box
	// 现今， 只考虑只有一个 视频box
	while (curr < end)
	{
		uint64_t        box_len = read_u32(curr);
		get_box_type(curr+4, box_type);

		if (memcmp(box_type, "minf", 4) == 0) {
			minf_box_parse(curr, box_len, mp4_moov_box);
			curr += box_len;
			break;
		}
		else {
			curr += box_len;
		}
	}
	return 0;
}

int trak_box_parse(uint8_t *trak, uint32_t box_size, st_mp4_moov_box *mp4_moov_box)
{

	char box_type[4] = { 0 };

	uint8_t *curr = trak + 8;
	const uint8_t *     end = trak + box_size;

	//目标：找到mdia box
	// 现今， 只考虑只有一个 视频box
	while (curr < end)
	{
		uint64_t        box_len = read_u32(curr);
		get_box_type(curr+4, box_type);

		if (memcmp(box_type, "mdia", 4) == 0) {
			mdia_box_parse(curr, box_len, mp4_moov_box);
			curr += box_len;
			break;
		}
		else {
			curr += box_len;
		}
	}
	return 0;
}

int moov_box_parse(uint8_t *moov, uint32_t moov_size, st_mp4_moov_box *mp4_moov_box)
{
	memset(mp4_moov_box, 0x00, sizeof(st_mp4_moov_box));

	char box_type[4] = { 0 };

	mp4_moov_box->moov_buf = moov;

	uint8_t *curr = moov + 8;
	const uint8_t *     end = moov + moov_size;

	//目标：知道track box
	// 现今， 只考虑只有一个 视频box
	while (curr < end)
	{
		uint64_t        box_len = read_u32(curr);
		get_box_type(curr+4, box_type);

		if (memcmp(box_type, "trak", 4) == 0) {
			trak_box_parse(curr, box_len, mp4_moov_box);
			curr += box_len;
			break;
		}
		else {
			curr += box_len;
		}
	}

	return 0;

}

// 获取每个帧数的大小
int stsz_box_parse(uint8_t *box, uint32_t box_size, uint32_t *sample_size_list)
{
	// todo 需要验证异常？ 其他box同样

	uint8_t *curr = box;
	curr += 4;  // 跳过大小
	curr += 4;  // type
	curr += 4;  // version 8bit + flag 24bit
	curr += 4;  // samplesize 只有所有sample一样大小数才有效

	int frameNum = read_u32(curr);

	if (sample_size_list == NULL) {
		return frameNum;
	}

	curr += 4;
	for (int i = 0; i < frameNum; ++i) {
		sample_size_list[i] = read_u32(curr);
		curr += 4;
		printf("%d \n", sample_size_list[i]);
	}
    
    return frameNum;
}

// 获取视频数据的开始位置， 也就是第一个chunk的开始位置
int stco_box_parse(uint8_t *box, uint32_t box_size, uint32_t *first_chunk_offset)
{
	uint8_t *curr = box;
	curr += 4;  // 跳过大小
	curr += 4;  // type
	curr += 4;  // version 8bit + flag 24bit

	int chunk_num = read_u32(curr);
	curr += 4;

	if (chunk_num < 1) {
		return -1;
	}

	*first_chunk_offset = read_u32(curr);

	return chunk_num;
}

//---------------------------------------------------------------------
// 倒序后的moov的构造
//----------------------------------------------------------------------

int stco_box_create(uint8_t *box, uint32_t box_size, uint32_t first_chunk_offset, uint32_t *sample_size_list_new, uint32_t frame_num, uint8_t *box_new)
{
	// 拷贝moov_box头部
	// todo box长度 回头要修改
	memcpy(box_new, box, 12);			// type  +   size  + (version + flag)

	uint8_t * curr_new = box_new + 12;

	// 只有一个chunk

	write_u32(curr_new, 1);				// chunk 数
	curr_new += 4;

	write_u32(curr_new, first_chunk_offset);				//chunk offset
	curr_new += 4;

	uint32_t box_size_new = curr_new - box_new;
	write_u32(box_new, box_size_new);

	return box_size_new;
}

int stsz_box_create(uint8_t *box, uint32_t box_size, uint32_t first_chunk_offset, uint32_t *sample_size_list_new, uint32_t frame_num, uint8_t *box_new)
{
	// 拷贝moov_box头部
	// todo box长度 回头要修改
	memcpy(box_new, box, 16);			// type  +   size  + (version + flag) + sample size(0)

	uint8_t * curr_new = box_new + 16;

	// frame 数
	write_u32(curr_new, frame_num);				//frame_num
	curr_new += 4;

	for (int i = 0; i < frame_num; ++i) {
		write_u32(curr_new, sample_size_list_new[i]);
		curr_new += 4;
	}

	uint32_t box_size_new = curr_new - box_new;
	write_u32(box_new, box_size_new);

	return box_size_new;
}

int stsc_box_create(uint8_t *box, uint32_t box_size, uint32_t first_chunk_offset, uint32_t *sample_size_list_new, uint32_t frame_num, uint8_t *box_new)
{
	// 拷贝moov_box头部
	// todo box长度 回头要修改
	memcpy(box_new, box, 12);			// type  +   size  + (version + flag)

	uint8_t * curr_new = box_new + 12;

	// 只有一个chunk

	write_u32(curr_new, 1);				// chunk 数
	curr_new += 4;

	write_u32(curr_new, 1);				//first chunk id
	curr_new += 4;
	write_u32(curr_new, frame_num);		// samples per chunk   每个chunk都会有这么多samples，知道 下一条chunk的定义
	curr_new += 4;
	write_u32(curr_new, 1);				// sample desription index  (现在固定1， todo，也是一个风险项)
	curr_new += 4;

	uint32_t box_size_new = curr_new - box_new;
	write_u32(box_new, box_size_new);

	return box_size_new;
}

int stbl_box_create(uint8_t *box, uint32_t box_size, uint32_t first_chunk_offset, uint32_t *sample_size_list_new, uint32_t frame_num, uint8_t *box_new)
{

	char box_type[4] = { 0 };

	// 拷贝moov_box头部
	// todo box长度 回头要修改
	memcpy(box_new, box, 8);

	uint8_t *curr = box + 8;
	const uint8_t *     end = box + box_size;

	uint8_t * curr_new = box_new + 8;

	//目标：找到mdia box
	// 现今， 只考虑只有一个 视频box
	while (curr < end)
	{
		uint64_t        box_len = read_u32(curr);
		get_box_type(curr + 4, box_type);
		//const uint8_t * box_data = curr + 8;

		if (memcmp(box_type, "stts", 4) == 0) {
			memcpy(curr_new, curr, box_len);
			curr += box_len;
			curr_new += box_len;
		}
		else if (memcmp(box_type, "stsc", 4) == 0) {
			int box_len_new = stsc_box_create(curr, box_len, first_chunk_offset, sample_size_list_new, frame_num, curr_new);
			curr += box_len;
			curr_new += box_len_new;
		}
		else if (memcmp(box_type, "stsz", 4) == 0) {
			int box_len_new = stsz_box_create(curr, box_len, first_chunk_offset, sample_size_list_new, frame_num, curr_new);
			curr += box_len;
			curr_new += box_len_new;
		}
		else if (memcmp(box_type, "stco", 4) == 0) {
			int box_len_new = stco_box_create(curr, box_len, first_chunk_offset, sample_size_list_new, frame_num, curr_new);
			curr += box_len;
			curr_new += box_len_new;
		}
		else {
			memcpy(curr_new, curr, box_len);
			curr += box_len;
			curr_new += box_len;
		}
	}

	uint32_t box_size_new = curr_new - box_new;
	write_u32(box_new, box_size_new);

	return box_size_new;
}


//返回box大小
int minf_box_create(uint8_t *box, uint32_t box_size, uint32_t first_chunk_offset, uint32_t *sample_size_list_new, uint32_t frame_num, uint8_t *box_new)
{

	char box_type[4] = { 0 };

	// 拷贝moov_box头部
	// todo box长度 回头要修改
	memcpy(box_new, box, 8);

	uint8_t *curr = box + 8;
	const uint8_t *     end = box + box_size;

	uint8_t * curr_new = box_new + 8;

	//目标：找到mdia box
	// 现今， 只考虑只有一个 视频box
	while (curr < end)
	{
		uint64_t        box_len = read_u32(curr);
		get_box_type(curr + 4, box_type);

		if (memcmp(box_type, "stbl", 4) == 0) {
			int box_len_new = stbl_box_create(curr, box_len, first_chunk_offset, sample_size_list_new, frame_num, curr_new);
			curr += box_len;
			curr_new += box_len_new;
			//break;
		}
		else {
			memcpy(curr_new, curr, box_len);
			curr += box_len;
			curr_new += box_len;
		}
	}

	uint32_t box_size_new = curr_new - box_new;
	write_u32(box_new, box_size_new);

	return box_size_new;
}

//返回box大小
int mdia_box_create(uint8_t *box, uint32_t box_size, uint32_t first_chunk_offset, uint32_t *sample_size_list_new, uint32_t frame_num, uint8_t *box_new)
{

	char box_type[4] = { 0 };

	// 拷贝moov_box头部
	// todo box长度 回头要修改
	memcpy(box_new, box, 8);

	uint8_t *curr = box + 8;
	const uint8_t *     end = box + box_size;

	uint8_t * curr_new = box_new + 8;

	//目标：找到mdia box
	// 现今， 只考虑只有一个 视频box
	while (curr < end)
	{
		uint64_t        box_len = read_u32(curr);
		get_box_type(curr + 4, box_type);

		if (memcmp(box_type, "minf", 4) == 0) {
			int box_len_new = minf_box_create(curr, box_len, first_chunk_offset, sample_size_list_new, frame_num, curr_new);
			curr += box_len;
			curr_new += box_len_new;
			//break;
		}
		else {
			memcpy(curr_new, curr, box_len);
			curr += box_len;
			curr_new += box_len;
		}
	}

	uint32_t box_size_new = curr_new - box_new;
	write_u32(box_new, box_size_new);

	return box_size_new;
}

//返回box大小
int trak_box_create(uint8_t *box, uint32_t box_size, uint32_t first_chunk_offset, uint32_t *sample_size_list_new, uint32_t frame_num, uint8_t *box_new)
{

	char box_type[4] = { 0 };

	// 拷贝moov_box头部
	// todo box长度 回头要修改
	memcpy(box_new, box, 8);

	uint8_t *curr = box + 8;
	const uint8_t *     end = box + box_size;

	uint8_t * curr_new = box_new + 8;

	//目标：找到mdia box
	// 现今， 只考虑只有一个 视频box
	while (curr < end)
	{
		uint64_t        box_len = read_u32(curr);
		get_box_type(curr + 4, box_type);

		if (memcmp(box_type, "mdia", 4) == 0) {
			int box_len_new = mdia_box_create(curr, box_len, first_chunk_offset, sample_size_list_new, frame_num, curr_new);
			curr += box_len;
			curr_new += box_len_new;
			//break;
		}
		else {
			memcpy(curr_new, curr, box_len);
			curr += box_len;
			curr_new += box_len;
		}
	}

	uint32_t box_size_new = curr_new - box_new;
	write_u32(box_new, box_size_new);

	return box_size_new;
}

// 需要修改的只有以下box，但是不止内容改变，大小也会变
// stts:最终要改，暂时不改
// stsc: 每个chunk的sample数，  改为只有一个chunk
// stsz: 每个帧的大小，  原来的stsz倒序
// stco: 每个chunk的开始位置offset， 改为只有一个chunk，位置和原来一样
int moov_box_create(uint8_t *moov, uint32_t moov_size, uint32_t first_chunk_offset, uint32_t *sample_size_list_new, uint32_t frame_num, uint8_t *moov_new)
{

	char box_type[4] = { 0 };

	// 拷贝moov_box头部
	// todo box长度 回头要修改
	memcpy(moov_new, moov, 8);

	uint8_t *curr = moov + 8;
	const uint8_t *     end = moov + moov_size;

	uint8_t * curr_new = moov_new + 8;

	//目标：知道track box
	// 现今， 只考虑只有一个 视频box
	while (curr < end)
	{
		uint64_t        box_len = read_u32(curr);
		get_box_type(curr + 4, box_type);

		if (memcmp(box_type, "trak", 4) == 0) {
			int box_len_new = trak_box_create(curr, box_len, first_chunk_offset, sample_size_list_new, frame_num, curr_new);
			curr += box_len;
			curr_new += box_len_new;
			//break;
		}
		else {
			memcpy(curr_new, curr, box_len);
			curr += box_len;
			curr_new += box_len;
		}
	}

	uint32_t moov_size_new = curr_new - moov_new;
	write_u32(moov_new, moov_size_new);

	return 0;

}

//--------------------------------------------------------------------------------------------

//---------------------------------------------------------------------------------------------

int mp4_reverse_file_create(FILE *fpFrom, uint8_t * moov_box, uint32_t first_chunk_offset, uint32_t *sample_pos_list, uint32_t *sample_size_list,uint32_t frame_num, FILE *fpTo)
{
	uint32_t pos = 0;
	char box_type[4] = { 0 };

	uint8_t buf[D_FILE_READ_BUF_SIZE];

	// 获取文件大小（feof不好用）
	fseek(fpFrom, 0, SEEK_END);
	uint32_t file_size = ftell(fpFrom);


	fseek(fpFrom, 0, SEEK_SET);
	while (ftell(fpFrom) < file_size)
	{
		int head_size = 8; // 从box的开始位置算起

		uint64_t        box_len = read_u32_from_file(fpFrom);

		// mdat_box会比较大，但短视频应该不会出现这个情况
		if (box_len == 1) {
			box_len = read_u64_from_file(fpFrom);
			head_size += 8;
		}

		get_box_type_from_file(fpFrom, box_type);

		if (memcmp(box_type, "moov", 4) == 0) {
			uint32_t moov_size = read_u32(moov_box);
			fwrite(moov_box, 1, moov_size, fpTo);

			// 源文件，跳到下一个box的开始位置
			fseek(fpFrom, box_len -head_size, SEEK_CUR);

			uint32_t ppp = ftell(fpFrom);
			printf("pos1 %d \n", ppp);
		}
		else if (memcmp(box_type, "mdat", 4) == 0) {
			// box head直接拷贝
			fseek(fpFrom, -head_size, SEEK_CUR);
			// 记住位置，等下还要回来
			uint32_t pos_temp = ftell(fpFrom);
			fread(buf, 1, head_size, fpFrom);
			fwrite(buf, 1, head_size, fpTo);

			for (int i = frame_num - 1; i >= 0; --i) {
				uint32_t read_pos = sample_pos_list[i] + first_chunk_offset;
				fseek(fpFrom, read_pos, SEEK_SET);
				uint32_t read_size = sample_size_list[i];
				while (read_size > 0) {
					int size_tmp = D_FILE_READ_BUF_SIZE;
					if (read_size < size_tmp) {
						size_tmp = read_size;
					}
					fread(buf, 1, size_tmp, fpFrom);
					fwrite(buf, 1, size_tmp, fpTo);

					read_size -= size_tmp;
				}

			}

			fseek(fpFrom, pos_temp + box_len, SEEK_SET);
		}
		else {
			// 直接拷贝
			fseek(fpFrom, -head_size, SEEK_CUR);
			uint32_t read_size = box_len;
			while (read_size > 0) {
				int size_tmp = D_FILE_READ_BUF_SIZE;
				if (read_size < size_tmp) {
					size_tmp = read_size;
				}
				fread(buf, 1, size_tmp, fpFrom);
				fwrite(buf, 1, size_tmp, fpTo);

				read_size -= size_tmp;
			}
		}
	}

	return pos;
}

//---------------------------------------------------------------------------------------------


// 释放资源

void free_resource(FILE *pFileFrom, FILE *pFileTo, uint8_t *moov_buf, uint32_t *sample_size_list, uint8_t *moov_buf_new)
{
	if (moov_buf != NULL) {
		free(moov_buf);
		moov_buf = NULL;
	}

	if (sample_size_list != NULL) {
		free(sample_size_list);
		sample_size_list = NULL;
	}

	if (moov_buf_new != NULL) {
		free(moov_buf_new);
		moov_buf_new = NULL;
	}

	if (pFileFrom != NULL) {
		fclose(pFileFrom);
		pFileFrom = NULL;
	};
	if (pFileTo != NULL) {
		fclose(pFileTo);
		pFileTo = NULL;
	};
}

int mp4_reverse(const char *filePathFrom, const char *filePathTo)
{
	int result = 0;

	FILE *pFileFrom = NULL;
	FILE *pFileTo = NULL;

	uint8_t *moov_buf = NULL;
	uint32_t *sample_size_list = NULL;

	uint8_t *moov_buf_new = NULL;

	// 因为用了goto，变量必须在前面



	pFileFrom = fopen(filePathFrom, "rb");
	if (pFileFrom == NULL)
	{
		// 文件打开失败
		result = -1;
		free_resource(pFileFrom, pFileTo, moov_buf, sample_size_list, moov_buf_new);
		return result;
	}

	pFileTo = fopen(filePathTo, "wb");
	if (pFileTo == NULL)
	{
		// 文件打开失败
		result = -1;
		free_resource(pFileFrom, pFileTo, moov_buf, sample_size_list, moov_buf_new);
		return result;
	}

	uint32_t moov_size = 0;
	// 定位moov box的 位置
	uint32_t moov_pos = get_moov_box_pos(pFileFrom, &moov_size);

	if (moov_size == 0) {
		result = -2;
		free_resource(pFileFrom, pFileTo, moov_buf, sample_size_list, moov_buf_new);
		return result;
	}

	// 分配moov用的buf
	moov_buf = (uint8_t*)malloc(moov_size);
	if (moov_buf == NULL) {
		result = -3;
		free_resource(pFileFrom, pFileTo, moov_buf, sample_size_list, moov_buf_new);
		return result;
	}

	// 读取moov
	fseek(pFileFrom, moov_pos, SEEK_SET);

	int size = fread(moov_buf, 1, moov_size, pFileFrom);
	if (size != moov_size) {
		result = -4;
		free_resource(pFileFrom, pFileTo, moov_buf, sample_size_list, moov_buf_new);
		return result;
	}


	// 解析moov
	st_mp4_moov_box mp4_moov_box;
	moov_box_parse(moov_buf, moov_size, &mp4_moov_box);

	
	uint32_t frame_num = stsz_box_parse(mp4_moov_box.stsz_buf, 0, sample_size_list);
	if (frame_num > 0) {
		sample_size_list = (uint32_t *)malloc(sizeof(uint32_t) * frame_num * 4);
	}

	if (sample_size_list == NULL) {
		result = -5;
		free_resource(pFileFrom, pFileTo, moov_buf, sample_size_list, moov_buf_new);
		return result;
	}

	stsz_box_parse(mp4_moov_box.stsz_buf, 0, sample_size_list);

	uint32_t *sample_pos_list = sample_size_list + frame_num;		// 各帧偏移位置（相对于第一个chunk，也就是第一帧是0）

	uint32_t *sample_size_list_new = sample_size_list + frame_num * 2;	// 倒序后各帧的大小
	uint32_t *sample_pos_list_new = sample_size_list + frame_num * 3;		// 倒序后各帧偏移位置（相对于第一个chunk，也就是第一帧是0）


	uint32_t first_chunk_offset = 0;

	int res = stco_box_parse(mp4_moov_box.stco_buf, 0, &first_chunk_offset);
	if (res < 0) {
		result = -6;
		free_resource(pFileFrom, pFileTo, moov_buf, sample_size_list, moov_buf_new);
		return result;
	}

	// 倒序后各帧的大小
	for (int i = 0; i < frame_num; ++i) {
		sample_size_list_new[i] = sample_size_list[frame_num - 1 - i];
	}

	//
	sample_pos_list[0] = 0;
	sample_pos_list_new[0] = 0;
	for (int i = 1; i < frame_num; ++i) {
		sample_pos_list[i] = sample_pos_list[i - 1] + sample_size_list[i - 1];
		sample_pos_list_new[i] = sample_pos_list_new[i - 1] + sample_size_list_new[i - 1];
	}

	// todo chunk暂设置为1个
	// 时间戳 也不变
	// 假定moov大小不会超过2M

	// 分配moov用的buf
	moov_buf_new = (uint8_t*)malloc(D_MOOV_BOX_MAX_SIZE);
	if (moov_buf_new == NULL) {
		result = -7;
		free_resource(pFileFrom, pFileTo, moov_buf, sample_size_list, moov_buf_new);
		return result;
	}

	moov_box_create(moov_buf, moov_size, first_chunk_offset, sample_size_list_new, frame_num, moov_buf_new);


	//

	mp4_reverse_file_create(pFileFrom, moov_buf_new, first_chunk_offset, sample_pos_list, sample_size_list, frame_num, pFileTo);

	free_resource(pFileFrom, pFileTo, moov_buf, sample_size_list, moov_buf_new);
	return result;
}


//-------------------------------------------------------------------------------------------------------------------------

// 不知道具体规则，按实际生成的文件来写
int udta_hdlr_box_create(uint8_t *box_new, const char* tag_file)
{
	uint8_t * curr_new = box_new;

	// box大小，先随便写一个
	write_u32(curr_new, 34);
	curr_new += 4;

	write_box_type(curr_new, "hdlr");
	curr_new += 4;

	write_u32(curr_new, 0);	//version + flag
	curr_new += 4;

	write_u32(curr_new, 0);	// predfiend
	curr_new += 4;
	
	write_box_type(curr_new, "mdir");	// handle type
	curr_new += 4;
	write_box_type(curr_new, "appl");	// reserver 12byte	
	curr_new += 4;

	write_u32(curr_new, 0);
	curr_new += 4;

	write_u32(curr_new, 0);
	curr_new += 4;

	*(curr_new++) = 0x00;		// name, 不定 '\0'结尾
	*(curr_new++) = 0x00;


	uint32_t box_size_new = curr_new - box_new;
	write_u32(box_new, box_size_new);

	return box_size_new;
}

int udta_data_box_create(uint8_t *box_new, uint32_t data_type, const char* data, uint32_t data_size)
{
	uint8_t * curr_new = box_new;

	// box大小，先随便写一个
	write_u32(curr_new, 8);
	curr_new += 4;

	write_box_type(curr_new, "data");
	curr_new += 4;

	write_u32(curr_new, data_type);
	curr_new += 4;

	write_u32(curr_new, 0);
	curr_new += 4;

	memcpy(curr_new, data, data_size);
	curr_new += data_size;

	uint32_t box_size_new = curr_new - box_new;
	write_u32(box_new, box_size_new);

	return box_size_new;
}

int udta_cpil_box_create(uint8_t *box_new, const char* tag_file)
{
	uint8_t * curr_new = box_new;

	// box大小，先随便写一个
	write_u32(curr_new, 8);
	curr_new += 4;

	write_box_type(curr_new, "cpil");
	curr_new += 4;

	char data[8] = { 0 };

	int box_len_new = udta_data_box_create(curr_new, 21, data, 1);
	curr_new += box_len_new;

	uint32_t box_size_new = curr_new - box_new;
	write_u32(box_new, box_size_new);

	return box_size_new;
}

int udta_rati_box_create(uint8_t *box_new, const char* tag_file)
{
	uint8_t * curr_new = box_new;

	// box大小，先随便写一个
	write_u32(curr_new, 8);
	curr_new += 4;

	write_box_type(curr_new, "rati");
	curr_new += 4;

	char data[8] = { 0 };

	int box_len_new = udta_data_box_create(curr_new, 0, data, 2);
	curr_new += box_len_new;

	uint32_t box_size_new = curr_new - box_new;
	write_u32(box_new, box_size_new);

	return box_size_new;
}

int udta_stik_box_create(uint8_t *box_new, const char* tag_file)
{
	uint8_t * curr_new = box_new;

	// box大小，先随便写一个
	write_u32(curr_new, 8);
	curr_new += 4;

	write_box_type(curr_new, "stik");
	curr_new += 4;

	char data[8] = { 0 };

	int box_len_new = udta_data_box_create(curr_new, 21, data, 1);
	curr_new += box_len_new;

	uint32_t box_size_new = curr_new - box_new;
	write_u32(box_new, box_size_new);

	return box_size_new;
}

int udta_covr_box_create(uint8_t *box_new, const char* tag_file)
{
	uint8_t * curr_new = box_new;

	// box大小，先随便写一个
	write_u32(curr_new, 8);
	curr_new += 4;

	write_box_type(curr_new, "covr");
	curr_new += 4;

	FILE *fp = fopen(tag_file, "rb");
	if (fp == NULL) {
		return 0;
	}
	fseek(fp, 0, SEEK_END);
	uint32_t file_len = ftell(fp);

	char *data = (char *)malloc(file_len);
	if (data == NULL) {
		return 0;
	}

	fseek(fp, 0, SEEK_SET);

	int read_size = fread(data, 1, file_len, fp);
	if (read_size != file_len) {
		free(data);
		fclose(fp);
		return 0;
	}

	int box_len_new = udta_data_box_create(curr_new, 14, data, file_len); //png
	curr_new += box_len_new;

	uint32_t box_size_new = curr_new - box_new;
	write_u32(box_new, box_size_new);

	free(data);
	fclose(fp);

	return box_size_new;
}

int udta_ilst_box_create(uint8_t *box_new, const char* tag_file)
{
	uint8_t * curr_new = box_new;

	// box大小，先随便写一个
	write_u32(curr_new, 8);
	curr_new += 4;

	write_box_type(curr_new, "ilst");
	curr_new += 4;

	int box_len_new = udta_cpil_box_create(curr_new, tag_file);
	curr_new += box_len_new;

	box_len_new = udta_rati_box_create(curr_new, tag_file);
	curr_new += box_len_new;

	box_len_new = udta_stik_box_create(curr_new, tag_file);
	curr_new += box_len_new;

	box_len_new = udta_covr_box_create(curr_new, tag_file);
	curr_new += box_len_new;

	uint32_t box_size_new = curr_new - box_new;
	write_u32(box_new, box_size_new);

	return box_size_new;
}

int udta_meta_box_create(uint8_t *box_new, const char* tag_file)
{
	uint8_t * curr_new = box_new;

	// box大小，先随便写一个
	write_u32(curr_new, 8);
	curr_new += 4;

	write_box_type(curr_new, "meta");
	curr_new += 4;

	write_u32(curr_new, 0); //version + flag
	curr_new += 4;

	int box_len_new = udta_hdlr_box_create(curr_new, tag_file);
	curr_new += box_len_new;

	box_len_new = udta_ilst_box_create(curr_new, tag_file);
	curr_new += box_len_new;

	//free box不要了。

	uint32_t box_size_new = curr_new - box_new;
	write_u32(box_new, box_size_new);

	return box_size_new;
}

//返回box大小
//不同于前面的函数，这个box是从头开始创建
int udta_box_create(uint8_t *box_new, const char* tag_file)
{

	char box_type[4] = { 0 };

	uint8_t * curr_new = box_new;

	// box大小，先随便写一个
	write_u32(curr_new, 8);
	curr_new += 4;

	write_box_type(curr_new, "udta");
	curr_new += 4;

	int box_len_new = udta_meta_box_create(curr_new, tag_file);
	curr_new += box_len_new;


	uint32_t box_size_new = curr_new - box_new;
	write_u32(box_new, box_size_new);

	return box_size_new;
}

// 需要修改的只有以下box，但是不止内容改变，大小也会变
// 添加udta
int moov_box_create_for_tag(uint8_t *moov, uint32_t moov_size, const char* tag_file, uint8_t *moov_new)
{

	char box_type[4] = { 0 };

	// 拷贝moov_box头部
	// todo box长度 回头要修改
	memcpy(moov_new, moov, 8);

	uint8_t *curr = moov + 8;
	const uint8_t *     end = moov + moov_size;

	uint8_t * curr_new = moov_new + 8;

	//目标：知道track box
	// 现今， 只考虑只有一个 视频box
	while (curr < end)
	{
		uint64_t        box_len = read_u32(curr);
		get_box_type(curr + 4, box_type);

		if (memcmp(box_type, "udta", 4) == 0) {
			//跳过
			curr += box_len;
			curr_new += 0;
			//break;
		}
		else {
			memcpy(curr_new, curr, box_len);
			curr += box_len;
			curr_new += box_len;
		}
	}

	// 添加udta box

	int box_len_new = udta_box_create(curr_new, tag_file);
	curr_new += box_len_new;


	uint32_t moov_size_new = curr_new - moov_new;
	write_u32(moov_new, moov_size_new);

	return 0;

}


int mp4_tag_file_create(FILE *fpFrom, uint8_t * moov_box, FILE *fpTo)
{
	uint32_t pos = 0;
	char box_type[4] = { 0 };

	uint8_t buf[D_FILE_READ_BUF_SIZE];

	// 获取文件大小（feof不好用）
	fseek(fpFrom, 0, SEEK_END);
	uint32_t file_size = ftell(fpFrom);


	fseek(fpFrom, 0, SEEK_SET);
	while (ftell(fpFrom) < file_size)
	{
		int head_size = 8; // 从box的开始位置算起

		uint64_t        box_len = read_u32_from_file(fpFrom);

		// mdat_box会比较大，但短视频应该不会出现这个情况
		if (box_len == 1) {
			box_len = read_u64_from_file(fpFrom);
			head_size += 8;
		}

		get_box_type_from_file(fpFrom, box_type);

		if (memcmp(box_type, "moov", 4) == 0) {
			uint32_t moov_size = read_u32(moov_box);
			fwrite(moov_box, 1, moov_size, fpTo);

			// 源文件，跳到下一个box的开始位置
			fseek(fpFrom, box_len - head_size, SEEK_CUR);

			uint32_t ppp = ftell(fpFrom);
			printf("pos1 %d \n", ppp);
		}
		else {
			// 直接拷贝
			fseek(fpFrom, -head_size, SEEK_CUR);
			uint32_t read_size = box_len;
			while (read_size > 0) {
				int size_tmp = D_FILE_READ_BUF_SIZE;
				if (read_size < size_tmp) {
					size_tmp = read_size;
				}
				fread(buf, 1, size_tmp, fpFrom);
				fwrite(buf, 1, size_tmp, fpTo);

				read_size -= size_tmp;
			}
		}
	}

	return pos;
}


int mp4_set_cover(const char* filePathFrom, const char *filePathTo, const char *tag_file)
{
	int result = 0;

	FILE *pFileFrom = NULL;
	FILE *pFileTo = NULL;

	uint8_t *moov_buf = NULL;
	uint32_t *sample_size_list = NULL;

	uint8_t *moov_buf_new = NULL;

	// 因为用了goto，变量必须在前面

	pFileFrom = fopen(filePathFrom, "rb");
	if (pFileFrom == NULL)
	{
		// 文件打开失败
		result = -1;
		free_resource(pFileFrom, pFileTo, moov_buf, sample_size_list, moov_buf_new);
		return result;
	}

	pFileTo = fopen(filePathTo, "wb");
	if (pFileTo == NULL)
	{
		// 文件打开失败
		result = -1;
		free_resource(pFileFrom, pFileTo, moov_buf, sample_size_list, moov_buf_new);
		return result;
	}

	uint32_t moov_size = 0;
	// 定位moov box的 位置
	uint32_t moov_pos = get_moov_box_pos(pFileFrom, &moov_size);

	if (moov_size == 0) {
		result = -2;
		free_resource(pFileFrom, pFileTo, moov_buf, sample_size_list, moov_buf_new);
		return result;
	}

	// 分配moov用的buf
	moov_buf = (uint8_t*)malloc(moov_size);
	if (moov_buf == NULL) {
		result = -3;
		free_resource(pFileFrom, pFileTo, moov_buf, sample_size_list, moov_buf_new);
		return result;
	}

	// 读取moov
	fseek(pFileFrom, moov_pos, SEEK_SET);

	int size = fread(moov_buf, 1, moov_size, pFileFrom);
	if (size != moov_size) {
		result = -4;
		free_resource(pFileFrom, pFileTo, moov_buf, sample_size_list, moov_buf_new);
		return result;
	}

	// 分配moov用的buf
	moov_buf_new = (uint8_t*)malloc(D_MOOV_BOX_MAX_SIZE);
	if (moov_buf_new == NULL) {
		result = -7;
		free_resource(pFileFrom, pFileTo, moov_buf, sample_size_list, moov_buf_new);
		return result;
	}

	moov_box_create_for_tag(moov_buf, moov_size, tag_file, moov_buf_new);


	mp4_tag_file_create(pFileFrom, moov_buf_new, pFileTo);

	free_resource(pFileFrom, pFileTo, moov_buf, sample_size_list, moov_buf_new);


	return result;
}
