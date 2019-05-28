#ifndef _CV2_MATRIX_H
#define _CV2_MATRIX_H

#include <iostream>
//#include <arm_neon.h>

namespace cv2
{
	// todo 假定所有参数都是合法的， 比如要求的地址不会越界， 矩阵的行列号

	// for memory debug
	//struct CountNum {
	//	static int num;
	//};
	
	struct Rect {
		Rect():x(0), y(0), width(0), height(0) {

		}
		Rect(int x, int y, int width, int height) {
			this->x = x;
			this->y = y;
			this->width = width;
			this->height = height;
		}
		int area() {
			return width * height;
		}
		friend Rect operator&(Rect& r1, Rect&r2);
		int x;
		int y;
		int width;
		int height;
	};

	Rect operator&(const Rect& r1, const Rect&r2);

	template<typename T>
	class Matrix2d;
	template<class S> Matrix2d<S> operator*(const Matrix2d<S> &v1, const Matrix2d<S> &v2);
	template<typename T>
	class Matrix2d
	{
	public:
		Matrix2d() : _rows(0), _cols(0), data(NULL),ref(NULL) {

		}

		Matrix2d(const Matrix2d<T> &m) {
			this->_rows = m._rows;
			this->_cols = m._cols;
			this->data = m.data;
			this->ref = m.ref;
            if( this->data != NULL )
                *ref += 1;
		}

		inline void create(int rows, int cols) {
			if (rows < 1 || cols < 1) {
				//error log
				return;
			}
			if (this->data != NULL) {
				if (*ref == 0) {
					//impossible
				}
				else if (*ref == 1) {
					*ref = 0;
					delete[] data;
					data = NULL;
					ref = NULL;
				}
				else {
					*ref -= 1;
				}
			}
			this->_rows = rows;
			this->_cols = cols;
			data = (T*)new char[rows*cols*sizeof(T) + 4 +4 ];
			memset(data, 0x00, rows*cols * sizeof(T) + 4);// TODO performance??
			ref = (int *)(data + rows*cols);
			*ref = 1;

//			{
//				*(ref + 1) = CountNum::num++;
//                std::cout << "new" << rows*cols * sizeof(T) + 4 << " " << *(ref+1) <<  std::endl;
//			}
		}

		inline int rows() const {
			return this->_rows;
		}

		inline int cols() const {
			return this->_cols;
		}

		inline T* getData() {
			return this->data;
		}
		
		inline const T* getData() const {
			return this->data;
		}

		bool empty() {
			return (data == NULL);
		}
		
		Matrix2d<T>& operator*(double value) {
			for (int i = 0; i < _rows * _cols; ++i) {
				data[i] *= value;
			}
			return *this;
		}

		Matrix2d<T>& operator+(const Matrix2d<T> &m) {
			if (this->_rows != m._rows || this->_cols != m._cols) {
				// error log
				return *this;
			}
			for (int i = 0; i < _rows * _cols; ++i) {
				data[i] += m.data[i];
			}
			return *this;
		}

		Matrix2d<T>& operator=(const Matrix2d<T> &m) {
			if (this != &m) {
				if (data != NULL) {
					if (*ref == 0) {
						//impossible
					}
					else if (*ref == 1) {
						*ref = 0;
//                        std::cout << "delete  operator = " << *(ref + 1) << std::endl;
						delete[] data;
						data = NULL;
						ref = NULL;
					}
					else {
						*ref -= 1;
					}
				}

				this->_rows = m._rows;
				this->_cols = m._cols;
				this->data = m.data;
				this->ref = m.ref;
				if (m.data != NULL) {
					*ref += 1;
				}
			}

			
			return *this;
		}

		T& operator()(int rowsIndex, int colsIndex) {
			return data[this->_cols * rowsIndex + colsIndex];
		}

		//todo test need todo
		~Matrix2d() {
			//todo 多线程
			if (data != NULL) {
				if (*ref == 0) {
					//impossible
				}
				else if (*ref == 1) {
					*ref = 0;
//                    std::cout << "delete  operator = " << *(ref + 1) << std::endl;
					delete[] data;
					data = NULL;
					ref = NULL;
				}
				else {
					*ref -= 1;
				}
			}

			
		}

		void minMaxLoc(int startRow, int endRow, int startCol, int endCol, T& minVal, T& maxVal) {
			minVal = this->operator()(startRow, startCol);
			maxVal = this->operator()(startRow, startCol);
			for (int i = startRow; i < endRow; ++i) {
				for (int j = startCol; j < endCol; ++j) {
					if (this->operator()(i, j) < minVal) {
						minVal = this->operator()(i, j);
					}
					else if (this->operator()(i, j) > maxVal) {
						maxVal = this->operator()(i, j);
					}
				}
			}
		}
		Matrix2d<T> clone()
		{
			Matrix2d<T> temp;
			temp.create(this->_rows, this->_cols);
			memcpy(temp.data, this->data, _rows * _cols * sizeof(T));
			return temp;
		}
        Matrix2d<T>& cloneFrom(const Matrix2d<T>& from, int x, int y, int w, int h, int startX, int startY)
        {
            //Matrix2d<T> temp;
            //temp.create(w, h);
            //this->_cols = w;
            //this->_rows = h;
            memset(this->data, 0x00, this->_cols * this->_rows * sizeof(T));
            for (int r = 0; r < h; ++r) {
                if (w > 0) {
                    memcpy(this->data + this->_cols *(startY + r) + startX, from.data + (from.cols()*(y + r) + x), w * sizeof(T));
                }
                
            }
            return *this;
        }
        Matrix2d<T>& cloneFrom(const Matrix2d<T>& from, int x, int y, int w, int h)
        {
            this->_cols = w;
            this->_rows = h;
            for (int r = 0; r < this->_rows; ++r) {
                memcpy(this->data + this->_cols *r, from.data + (from.cols()*(y + r) + x), this->_cols * sizeof(T));
            }
            return *this;
        }
		Matrix2d<T> clone(int x, int y, int w, int h)
		{
			Matrix2d<T> temp;
			temp.create(w, h);
			for (int r = 0; r < temp.rows(); ++r) {
				memcpy(temp.data + temp.cols() *r, this->data + (this->cols()*(y + r) + x), temp.cols() * sizeof(T));
			}
			return temp;
		}

		// 固定填充0
		Matrix2d<T> copyMakeBorder(int borderTop, int borderBottom, int borderLeft, int borderRight)
		{
			Matrix2d<T> temp;
			int r = this->rows() + borderTop + borderBottom;
			int c = this->cols() + borderLeft + borderRight;
			temp.create(r, c);	//默认会初期化0
			for (int r = 0; r < this->rows(); ++r) {
				//memcpy(temp.data + temp.cols() * sizeof(T)*r, this->data + (this->cols()*(startRow + r) + startCol) * sizeof(T), temp.cols() * sizeof(T));
				memcpy(temp.data + (temp.cols() * (borderTop + r) + borderLeft), this->data + this->cols()*r, this->cols() * sizeof(T));
			}
			return temp;
		}

		//Matrix2d<float> resize_and_convert(int rows, int cols) {
		//	double scaleX = this->rows() / (double)rows;
		//	double scaleY = this->cols() / (double)cols;
		//	Matrix2d<float> temp;
		//	temp.create(rows, cols);
		//	for (int r = 0; r < rows; ++r) {
		//		for (int c = 0; c < cols; ++c) {
		//			double newX = r * scaleX;
		//			//newX = newX > (this->rows() - 1) ? (this->rows() - 1) : newX;
		//			double newY = c * scaleY;
		//			//newY = newY >(this->cols() - 1) ? (this->cols() - 1) : newY;
		//			int x1 = (int)newX;
		//			int y1 = (int)newY;
		//			//double rx = newX - x1;
		//			//double ry = newY - y1;
		//			//temp(r, c) = (this->operator()(x1, y1)*(1 - rx) + this->operator()(x1 + 1, y1)*rx) * (1 - ry) + (this->operator()(x1, y1+1)*(1 - rx) + this->operator()(x1 + 1, y1+1)*rx) * ry;
		//			temp(r, c) = this->data[x1*this->_cols + y1];//this->operator()(x1, y1);//
		//		}
		//	}
		//	return temp;
		//}

		// 这边其实矩阵的横竖搞错了，不过用的矩阵rows=cols所以问题不大
		void resize_and_convert(int rows, int cols, float* outdata) {
			double scaleX = this->rows() / (double)rows;
			double scaleY = this->cols() / (double)cols;
			//Matrix2d<float> temp;
			double newX = 0;
			//temp.create(rows, cols);
			for (int r = 0; r < rows; ++r) {
				//newX = scaleX * r;
				//newX = newX > (this->rows() - 1) ? (this->rows() - 1) : newX;
				int x1 = (int)newX;
				int rowPos = x1*this->_cols;
				double newY = 0;
				for (int c = 0; c < cols; ++c) {

					//newY = scaleY * c;
					//newY = newY >(this->cols() - 1) ? (this->cols() - 1) : newY;

					int y1 = (int)newY;
					//double rx = newX - x1;
					//double ry = newY - y1;
					//temp(r, c) = (this->operator()(x1, y1)*(1 - rx) + this->operator()(x1 + 1, y1)*rx) * (1 - ry) + (this->operator()(x1, y1+1)*(1 - rx) + this->operator()(x1 + 1, y1+1)*rx) * ry;
					//temp(r, c) = this->data[rowPos + y1];//this->operator()(x1, y1);//
					outdata[r*cols+c] = this->data[rowPos + y1];
					newY += scaleY;
				}
				newX += scaleX;
			}
			//return temp;
		}

		template<typename S> friend Matrix2d<S> operator*(const Matrix2d<S> &v1, const Matrix2d<S> &v2);
		//friend copyMakeBorder(extendedImage, extendedImage, borderTop, borderBottom, borderLeft, borderRight, cv::BORDER_CONSTANT, cv::Scalar(0));
        
	private:
		int _rows;
		int _cols;
		T * data;
		int *ref;
	};
    /*
    template<typename T>
    Matrix2d<T> operator*(Matrix2d<T> &v1, Matrix2d<T> &v2)
    {
        // m*n, n*k
        Matrix2d<T> temp;
        temp.create(v1.rows(), v2.cols());
        
        T* v1_data = v1.getData();
        T* v2_data = v2.getData();
        T* temp_data = temp.getData();
        
        int v1_rows = v1.rows();
        int v1_cols = v1.cols();
        int v2_cols = v2.cols();
        
        for (int m = 0; m < v1_rows; ++m) {
            
            for (int n = 0; n < v1_cols; ++n) {
                
                T r = v1_data[v1_cols * m + n];
                
                int temp_offset = v2_cols * m;
                int v2_offset = v2_cols * n;
                
                T * temp_offset_data = &(temp_data[temp_offset]);
                T * v2_offset_data = &(v2_data[v2_offset]);
                
                if (0 || sizeof(T) != 4) {
                    for (int k = 0; k < v2_cols; ++k) {
                        
                        //                    temp_data[v2_cols * m + k] += r * v2_data[v2_cols * n + k];
                        //                    temp_data[temp_offset + k] += r * v2_data[v2_offset + k];
                        *temp_offset_data = *temp_offset_data + r * (*v2_offset_data);
                        
                        temp_offset_data++;
                        v2_offset_data++;
                    }
                }
                else {
                    float r4[4] = { r, r, r, r };
                    for (int k = 0; k < v2_cols; k += 4) {
                        const float32x4_t val4 = vld1q_f32(v2_offset_data);
                        float32x4_t dry4 = vld1q_f32(r4);
                        
                        float32x4_t aaa = vld1q_f32(temp_offset_data);
                        //aaa = vmlaq_f32(aaa, val4, r4);
                        aaa = vaddq_f32(aaa, vmulq_f32(val4, dry4));
                        vst1q_f32(temp_offset_data, aaa);
                        temp_offset_data+=4;
                        v2_offset_data+=4;
                    }
                }
                
            }
        }
        
        return temp;
    }
    */
    template<typename T>
    Matrix2d<T> operator*(const Matrix2d<T> &v1, const Matrix2d<T> &v2)
    {
        // m*n, n*k
        Matrix2d<T> temp;
        temp.create(v1.rows(), v2.cols());
        //        for (int m = 0; m < v1.rows(); ++m) {
        //            for (int k = 0; k < v2.cols(); ++k) {
        //                temp(m, k) = 0;
        //                for (int n = 0; n < v1.cols(); ++n) {
        //                    temp(m, k) += v1(m, n) *v2(n, k);
        //                }
        //            }
        //        }
        
        const T* v1_data = v1.getData();
        const T* v2_data = v2.getData();
        T* temp_data = temp.getData();
        
        int v1_rows = v1.rows();
        int v1_cols = v1.cols();
        int v2_cols = v2.cols();
        
        for (int m = 0; m < v1_rows; ++m) {
            
            for (int n = 0; n < v1_cols; ++n) {
                
                T r = v1_data[v1_cols * m + n];
                
                int temp_offset = v2_cols * m;
                int v2_offset = v2_cols * n;
                
                T * temp_offset_data = &(temp_data[temp_offset]);
                const T * v2_offset_data = &(v2_data[v2_offset]);
                
                for (int k = 0; k < v2_cols; ++k) {
                    
                    //                    temp_data[v2_cols * m + k] += r * v2_data[v2_cols * n + k];
                    //                    temp_data[temp_offset + k] += r * v2_data[v2_offset + k];
                    *temp_offset_data = *temp_offset_data + r * (*v2_offset_data);
                    
                    temp_offset_data ++;
                    v2_offset_data ++;
                }

            }
        }
        
        return temp;
    }
	
	/*
	// 目前不使用，如果真要使用，还得考虑不是4的倍数的情况
	template< >
    inline Matrix2d<float> operator*(const Matrix2d<float> &v1, const Matrix2d<float> &v2)
    {
        // m*n, n*k
        Matrix2d<float> temp;
        temp.create(v1.rows(), v2.cols());
        //        for (int m = 0; m < v1.rows(); ++m) {
        //            for (int k = 0; k < v2.cols(); ++k) {
        //                temp(m, k) = 0;
        //                for (int n = 0; n < v1.cols(); ++n) {
        //                    temp(m, k) += v1(m, n) *v2(n, k);
        //                }
        //            }
        //        }
        
        const float* v1_data = v1.getData();
        const float* v2_data = v2.getData();
        float* temp_data = temp.getData();
        
        int v1_rows = v1.rows();
        int v1_cols = v1.cols();
        int v2_cols = v2.cols();
        
        for (int m = 0; m < v1_rows; ++m) {
            
            for (int n = 0; n < v1_cols; ++n) {
                
                float r = v1_data[v1_cols * m + n];
                
                int temp_offset = v2_cols * m;
                int v2_offset = v2_cols * n;
                
                float * temp_offset_data = &(temp_data[temp_offset]);
                const float * v2_offset_data = &(v2_data[v2_offset]);
                
                    float r4[4] = { r, r, r, r };
                    for (int k = 0; k < v2_cols; k += 4) {
                        const float32x4_t val4 = vld1q_f32(v2_offset_data);
                        float32x4_t dry4 = vld1q_f32(r4);
                        
                        float32x4_t aaa = vld1q_f32(temp_offset_data);
                        //aaa = vmlaq_f32(aaa, val4, r4);
                        aaa = vaddq_f32(aaa, vmulq_f32(val4, dry4));
                        vst1q_f32(temp_offset_data, aaa);
                        temp_offset_data+=4;
                        v2_offset_data+=4;
                    }

            }
        }
        
        return temp;
    }
	*/
}

#endif // !_CV2_MATRIX_H
