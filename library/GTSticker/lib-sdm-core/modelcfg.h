#ifndef MODELCFG_H_
#define MODELCFG_H_

#define LandmarkPointsNum  68

const char trainFilePath[] = "ibug_300W_large_face_landmark_dataset/";

const float mean_norm_shape[] = {-0.425447,-0.420378,-0.403986,-0.378397,-0.334865,-0.267835,-0.186091,-0.0926207,0.0171259,0.124622,0.217315,0.298924,0.364711,0.40539,0.424369,0.433527,0.434302,-0.346527,-0.291047,-0.219418,-0.146317,-0.0767238,0.0728158,0.145081,0.216934,0.287845,0.34347,0.00281427,0.00441905,0.00584405,0.00728284,-0.0733598,-0.0336527,0.00845645,0.0513711,0.0896271,-0.257595,-0.214834,-0.158869,-0.114167,-0.162445,-0.216635,0.120275,0.162891,0.21818,0.261672,0.224722,0.172206,-0.152704,-0.0922372,-0.0317683,0.00915638,0.0552207,0.117148,0.176954,0.121837,0.0623947,0.0129873,-0.0322064,-0.0926682,-0.127528,-0.0314446,0.0102627,0.0570615,0.151259,0.0582893,0.0107967,-0.0316032,-0.0945562,0.020082,0.133867,0.245261,0.349792,0.440875,0.518436,0.58143,0.597349,0.578498,0.513146,0.433165,0.339315,0.232338,0.117465,0.00277241,-0.11081,-0.189491,-0.233866,-0.244346,-0.23305,-0.20614,-0.211158,-0.241047,-0.253345,-0.245792,-0.208503,-0.119405,-0.0422907,0.0352936,0.114162,0.158849,0.174848,0.189675,0.173255,0.156922,-0.106242,-0.135285,-0.135007,-0.0995828,-0.0853928,-0.0851771,-0.104104,-0.141082,-0.143265,-0.115788,-0.0941629,-0.0917073,0.30736,0.279836,0.26671,0.276785,0.265286,0.276841,0.299686,0.359647,0.385563,0.391521,0.388128,0.364927,0.311043,0.30646,0.309391,0.304039,0.304872,0.328162,0.334493,0.330795};

const int LandmarkLength1 = 12;
const int IteraLandmarkIndex1[] = {3, 6, 8, 10, 13, 19, 24, 36, 45, 30, 48, 54};
const int LandmarkLength2 = 12;
const int IteraLandmarkIndex2[] = {3, 6, 8, 10, 13, 19, 24, 36, 45, 30, 48, 54};
const int LandmarkLength3 = 12;
const int IteraLandmarkIndex3[] = {3, 6, 8, 10, 13, 19, 24, 36, 45, 30, 48, 54};
const int LandmarkLength4 = 20;
const int IteraLandmarkIndex4[] = {3, 6, 8, 10, 13, 18, 20, 23, 25, 36, 39, 42, 45, 30, 31, 35, 48, 51, 54, 57};
const int LandmarkLength5 = 20;
const int IteraLandmarkIndex5[] = {3, 6, 8, 10, 13, 18, 20, 23, 25, 36, 39, 42, 45, 30, 31, 35, 48, 51, 54, 57};

const int eyes_indexs[4] = {36,39,42,45};

const int extern_point_Length = 14;
const int extern_point_indexs[] = {0,16,36,37,38,39,40,41,42,43,44,45,46,47};

const float estimateHeadPoseMat[] = {  -0.258801,-0.142125,0.445513,0.101524,-0.0117096,-0.119747,-0.426367,-0.0197618,-0.143073,
                                 -0.194121,-0.210882,0.0989902,0.0822748,-0.00544055,0.0184441,-0.0628809,-0.0944775,-0.162363,
                                 0.173311,-0.205982,0.105287,0.0767408,0.0101697,0.0156599,-0.0632534,0.0774872,0.139928,
                                 0.278776,-0.109497,0.537723,0.0488799,0.00548235,0.111033,-0.471475,0.0280982,0.157491,
                                 0.0427104,-0.348899,-1.95092,0.0493076,0.0340635,0.157101,2.01808,-0.0716708,0.0860774,
                                 -0.191908,0.551951,0.456261,-0.174833,-0.0202239,-0.203346,-0.575386,0.105571,-0.171957,
                                 0.150051,0.465426,0.307133,-0.183886,-0.0123275,0.0208533,-0.4187,-0.0252474,0.0939203,
                                 0.00521464,0.229863,0.0595028,-0.480886,-0.0684972,0.43404,-0.0206778,-0.428706,0.118848,
                                 0.0125229,0.140842,0.115793,-0.239542,-0.0933311,0.0913729,-0.106839,-0.0523733,0.0697435,
                                 0.030548,-0.101407,-0.0659365,0.220726,-0.113126,0.0189044,0.0785027,-0.02235,0.0964722,
                                 0.0143054,-0.274282,-0.173696,0.477843,-0.073234,0.297015,0.180833,-0.322039,0.0855057,
                                 0.117061,-0.00704583,0.0157153,0.00142929,-0.106156,-1.29549,-0.0134561,1.22806,0.048107,
                                 -0.0663207,0.0996722,0.0374666,-0.215455,0.240434,0.233645,-0.0148478,-0.144342,-0.175324,
                                 -0.113332,-0.0876358,0.011164,0.23588,0.213911,0.2205,-0.103526,-0.258239,-0.243352,
                                 0.535077,0.000906855,-0.0336819,0.015495,0.586095,-0.14663,0.0643886,-0.114478,0.937324};

#endif
