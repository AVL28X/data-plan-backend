# Data Plan Backend  ## Weekly Plan  [Google Doc Link](https://docs.google.com/a/west.cmu.edu/document/d/1zkj0hDP_ekbVFSIz1pqWxSyn-KzdJawjf_1K5gqY1Xg/edit?usp=sharing)  ## Architecture Diagram  ![Architecture Diagram](https://github.com/AVL28X/data-plan-backend/raw/master/Architecture%20Diagram.png)  ## User Parameter Estimation[PDF](https://github.com/AVL28X/data-plan-backend/raw/master/User%20Parameter%20Estimation.pdf)   ## Code Structure  * DataPlan.java  Class to model the data plan* User.java  Class to model the user, including properties like daily weights, alpha, phi and current data plan* Utilities.java  Utility functions for general purpose calculations, like calculate monthly utilities, parameters estimation* UserParamFitter  A class to estimate user's parameters including daily weights, phi and alpha using Levenberg-Marquardt Algorithm* DataPlanServer.java  GRPC Server that provides services like user parameter calibration, utility calculation and so on.* DataPlanClient.java  An example GRPC client that sends RPC requests for parameter estimation.## GRPC API  * User Parameter Estimation  ```proto// The data plan serviceservice DataPlanService {  rpc GetUserParam (UserParamRequest) returns (UserParamResponse) {} //service to calibrate user parameters  rpc GetRecommendUsages (RecommendUsagesRequest) returns(UsagesResponse) {}  //service to recommend daily usages  rpc GetUtility( UtilityRequest ) returns( UtilityResponse ){} //service to compute utitity of given user and dataplan  rpc GetRecommendedDataPlans( DataPlanRequest ) returns( DataPlanResponse ){} //service to recommend top data plans  rpc HelloWorld( HWRequest) returns( HWResponse ){}  //hello world for heathcheck}// Request for User Parameter Calibrationmessage UserParamRequest {  repeated Usage usages = 1;  double overage = 2;  // if overage > 0, heavy user, otherwise light user}// Response for User Parameter Calibrationmessage UserParamRequest {  UserParams userParams = 1;  UserParamsStd userParamsStd = 2;  // if overage > 0, heavy user, otherwise light user}// user parameters objectmessage UserParams {  double w1 = 1; //weights of each day of week  double w2 = 2;  double w3 = 3;  double w4 = 4;  double w5 = 5;  double w6 = 6;  double w7 = 7;  double phi = 8; // pi  double alpha = 9; //alpha}// user parameters stdmessage UserParamsStd {  double w1 = 1; //weights of each day of week  double w2 = 2;  double w3 = 3;  double w4 = 4;  double w5 = 5;  double w6 = 6;  double w7 = 7;  double phi = 8; // pi  double alpha = 9; //alpha}// Request for Daily Usages Recommendationmessage RecommendUsagesRequest{  int32 year = 1;  int32 month = 2;  UserParams userParams = 3;  DataPlanMsg dataPlan = 4;}// Response of recommended daily usagesmessage UsagesResponse{  repeated Usage usages = 1;}// Request for Utility Calculationmessage UtilityRequest{  UserParams userParams = 1;  DataPlanMsg dataPlan = 2;}// Response to Utility Calculation Requestmessage UtilityResponse{  double utility = 1;}//Request for top recommended data plansmessage DataPlanRequest{  UserParams userParams = 1;}//response of top data plansmessage DataPlanResponse{  repeated DataPlanMsg dataPlans = 1;}// Hello World Requestmessage HWRequest{  string word = 1;}// Hello World Responsemessage HWResponse{  string word = 1;}//object of daily usagemessage Usage {  int32 year = 2;  int32 month = 3;  int32 day = 4;  double usage = 1;}// object of data planmessage DataPlanMsg{  string name = 1;  string description = 2;  double quota = 3;  double overage = 4;  double price = 5;}```## Example Data Plans  [Data Plans.CSV](https://github.com/AVL28X/data-plan-backend/blob/master/Data%20Plans.csv)## Example Client Request and Response### HelloWorld  ```textword: "Hello from client"word: "Hello from server!"```### A pseudo-data plan for testing```textPseudo data plan createdquota: 1000.0overage: 0.01price: 35.0```### Response of Calibrated Parameters  ```textCalibrated Params:w1: 0.042539501125739794w2: 0.04091936098364235w3: 0.04309952622742607w4: 0.04104995612787689w5: 0.043283407774602364w6: 2.181604896733802E-11w7: 0.03910824773889646phi: 0.007839599730990358alpha: 0.29769491579217067Standard Deviation:w1: 0.0019487065819555071w2: 0.0020724956857322173w3: 0.0018883412859305403w4: 0.0017842397015089533w5: 0.001675321773520107w6: 0.0029064101994204306w7: 0.0022180573115046644phi: 4.565941540202295E-4alpha: 0.0040705960946803765```### Recommended Top 5 Data Plans```textRecommended Data Plansname: "Verizon Prepaid 7G"description: "https://www.verizonwireless.com/prepaid"quota: 7000.0overage: 0.02price: 50.0name: "Verizon Prepaid 3G"description: "https://www.verizonwireless.com/prepaid"quota: 3000.0overage: 0.02price: 40.0name: "AT&T Prepaid 6G"description: "https://www.att.com/prepaid/plans.html"quota: 6000.0overage: 0.01price: 45.0name: "AT&T Prepaid 1G"description: "https://www.att.com/prepaid/plans.html"quota: 1000.0overage: 0.01price: 35.0name: "Verizon Prepaid 10G"description: "https://www.verizonwireless.com/prepaid"quota: 10000.0overage: 0.02price: 60.0```### Resposne of Recommended Daily Usages  ```textRecommended Usages: usages {  usage: 16.921750044338605  year: 2017  month: 12  day: 1}usages {  usage: 2.0997563370221223E-29  year: 2017  month: 12  day: 2}usages {  usage: 33.50831641982961  year: 2017  month: 12  day: 3}usages {  usage: 42.23550960361205  year: 2017  month: 12  day: 4}usages {  usage: 50.632803266779696  year: 2017  month: 12  day: 5}usages {  usage: 44.262066249566885  year: 2017  month: 12  day: 6}usages {  usage: 49.832037799831035  year: 2017  month: 12  day: 7}usages {  usage: 16.921750044338605  year: 2017  month: 12  day: 8}usages {  usage: 2.0997563370221223E-29  year: 2017  month: 12  day: 9}usages {  usage: 33.50831641982961  year: 2017  month: 12  day: 10}usages {  usage: 42.23550960361205  year: 2017  month: 12  day: 11}usages {  usage: 50.632803266779696  year: 2017  month: 12  day: 12}usages {  usage: 44.262066249566885  year: 2017  month: 12  day: 13}usages {  usage: 49.832037799831035  year: 2017  month: 12  day: 14}usages {  usage: 16.921750044338605  year: 2017  month: 12  day: 15}usages {  usage: 2.0997563370221223E-29  year: 2017  month: 12  day: 16}usages {  usage: 33.50831641982961  year: 2017  month: 12  day: 17}usages {  usage: 42.23550960361205  year: 2017  month: 12  day: 18}usages {  usage: 50.632803266779696  year: 2017  month: 12  day: 19}usages {  usage: 44.262066249566885  year: 2017  month: 12  day: 20}usages {  usage: 49.832037799831035  year: 2017  month: 12  day: 21}usages {  usage: 16.921750044338605  year: 2017  month: 12  day: 22}usages {  usage: 2.0997563370221223E-29  year: 2017  month: 12  day: 23}usages {  usage: 33.50831641982961  year: 2017  month: 12  day: 24}usages {  usage: 42.23550960361205  year: 2017  month: 12  day: 25}usages {  usage: 50.632803266779696  year: 2017  month: 12  day: 26}usages {  usage: 44.262066249566885  year: 2017  month: 12  day: 27}usages {  usage: 49.832037799831035  year: 2017  month: 12  day: 28}usages {  usage: 16.921750044338605  year: 2017  month: 12  day: 29}usages {  usage: 2.0997563370221223E-29  year: 2017  month: 12  day: 30}usages {  usage: 33.50831641982961  year: 2017  month: 12  day: 31}```