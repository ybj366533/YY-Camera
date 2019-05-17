#include "matrix.h"
namespace cv2 {
	

	cv2::Rect operator&(const cv2::Rect& r1, const cv2::Rect&r2) {
		cv2::Rect rr;
		// 两个矩形没有相交情况
		if ((r1.x >= (r2.x + r2.width)) || (r1.y >= (r2.y + r2.height)) || ((r1.x + r1.width) < r2.x) || ((r1.y + r1.height) < r2.y)) {
			return rr;
		}

		rr.x = r1.x > r2.x ? r1.x : r2.x;
		rr.y = r1.y > r2.y ? r1.y : r2.y;

		if (r1.x + r1.width < r2.x + r2.width) {
			rr.width = r1.x + r1.width - rr.x;
		}
		else {
			rr.width = r2.x + r2.width - rr.x;
		}

		if (r1.y + r1.height < r2.y + r2.height) {
			rr.height = r1.y + r1.height - rr.y;
		}
		else {
			rr.height = r2.y + r2.height - rr.y;
		}
		return rr;
	}
}

