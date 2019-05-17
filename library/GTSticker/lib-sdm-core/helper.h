#ifndef __HELPER_H_
#define __HELPER_H_


#include "matrix.h"
#include "plat_log.h"
#include "math.h"

static cv2::Rect get_enclosing_bbox(cv2::Matrix2d<float> landmarks)
{
    auto num_landmarks = landmarks.cols() / 2;
    float min_x_val, max_x_val, min_y_val, max_y_val;
	landmarks.minMaxLoc(0, landmarks.rows(), 0, num_landmarks, min_x_val, max_x_val);
	landmarks.minMaxLoc(0, landmarks.rows(), num_landmarks, landmarks.cols(), min_y_val, max_y_val);
    float width  = max_x_val - min_x_val;
    float height =  max_y_val - min_y_val;
    return cv2::Rect(min_x_val, min_y_val, width, height);
}


/**
 * Performs an initial alignment of the model, by putting the mean model into
 * the center of the face box.
 *
 * An optional scaling and translation parameters can be given to generate
 * perturbations of the initialisation.
 *
 * Note 02/04/15: I think with the new perturbation code, we can delete the optional
 * parameters here - make it as simple as possible, don't include what's not needed.
 * Align and perturb should really be separate - separate things.
 *
 * @param[in] mean Mean model points.
 * @param[in] facebox A facebox to align the model to.
 * @param[in] scaling_x Optional scaling in x of the model.
 * @param[in] scaling_y Optional scaling in y of the model.
 * @param[in] translation_x Optional translation in x of the model.
 * @param[in] translation_y Optional translation in y of the model.
 * @return A cv::Mat of the aligned points.
 */

static cv2::Matrix2d<float> align_mean(cv2::Matrix2d<float> mean, cv2::Rect facebox, float scaling_x=1.0f, float scaling_y=1.0f, float translation_x=0.0f, float translation_y=0.0f)
{
	cv2::Matrix2d<float> aligned_mean = mean.clone();
	for (int i = 0; i < aligned_mean.cols() / 2; ++i) {
		aligned_mean(0, i) = (aligned_mean(0, i)*scaling_x + 0.5f + translation_x) * facebox.width + facebox.x;
	}
	
	for (int i = aligned_mean.cols() / 2; i < aligned_mean.cols(); ++i) {
		aligned_mean(0, i) = (aligned_mean(0, i)*scaling_y + 0.3f + translation_y) * facebox.height + facebox.y;
	}
	
    return aligned_mean;
}

static double calculateAngle(int x1, int y1, int x2, int y2)
{
    double dX = (double)(x2 - x1);
    double dY = (double)(y2 - y1);
    float angle = (float)(atan(dY / dX) * 180 / 3.1415926);
    
    if(dX < 0) {
        angle += 180;
    } else {
        // -90 --90;
    }
    
    if( angle < 0.0f ) {
        angle += 360.0f;
    }
    
    return angle;
}

static void getRotationMatrix2D( float centerX, float centerY, double angle, double scale, double *m)
{
    angle *= 3.14156/180;
    double alpha = cos(angle)*scale;
    double beta = sin(angle)*scale;
    
    m[0] = alpha;
    m[1] = beta;
    m[2] = (1-alpha)*centerX - beta*centerY;
    m[3] = -beta;
    m[4] = alpha;
    m[5] = beta*centerX + (1-alpha)*centerY;
    
}

#endif
