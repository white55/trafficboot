package com.xzst.traffic.controller;

import com.xzst.traffic.model.InputModel;
import com.xzst.traffic.model.OutputModel;
import com.xzst.traffic.model.Singleton;
import com.xzst.traffic.service.DataCleanService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.RVector;
import org.rosuda.JRI.Rengine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * Created by 张超 on 2017/9/29.
 */
@RestController
@RequestMapping(value="/data")
public class DataController {
    private Logger log = LoggerFactory.getLogger(DataController.class);
    @Resource
    private DataCleanService dataCleanService;
    @ApiOperation(value="数据清洗", notes="将输入的inputUrl.xxx.csv转换成算法需要的数据")
    @RequestMapping(value="/dataClean", method=RequestMethod.POST)
    @ResponseBody
    public String dataClean(@RequestParam @ApiParam(required = true, value = "inputUrl") String inputUrl, @RequestParam @ApiParam(required = true, value = "outputUrl") String outputUrl){
       List<InputModel> inputModels= dataCleanService.csvReader(inputUrl);
       List<OutputModel> outputModels=dataCleanService.DataConvert(inputModels);
       String flag=dataCleanService.csvWriter(outputModels,outputUrl);
        return flag;
    }
    @ApiOperation(value="训练", notes="用输入的inputUrl.xxx.csv训练算法，保存模型")
    @RequestMapping(value="/dataTrain", method=RequestMethod.POST)
    @ResponseBody
    public String dataTrain(@RequestParam @ApiParam(required = true, value = "inputUrl") String inputUrl,@RequestParam @ApiParam(required = true, value = "算法：0-lm,1-randomForest") int arithmeticType, @RequestParam @ApiParam(required = true, value = "outputUrl，模型保存地址（eg:e:/wuhuM1.RData）") String outputUrl){
        //Rengine re = new Rengine(new String[] { "--vanilla" }, false, null);
        //Rengine re=new Rengine();
        //Rengine R引擎，通过它进行R语言的启动、运算、画图、关闭等功能。
        //一个线程只能实例化一次，这里使用单例模式。
        Rengine re=Singleton.getSingleton();
        if (!re.waitForR()) {
            System.out.println("Cannot load R");
            return "R error：Cannot load R";
        }
        //打印变量
        String version = re.eval("R.version.string").asString();
        System.out.println(version);
        int arithmetic=0;
        arithmetic=arithmeticType;
        //lm
        if(arithmetic==0){
            // re.eval("wudata=read.table(\"E:/wri.csv\",header=TRUE,sep=\",\",quote=\"\\\"\")");
            re.eval("wudata=read.table(\""+inputUrl+"\",header=TRUE,sep=\",\",quote=\"\\\"\")");
            re.eval("library(\"nnet\")");
            re.eval("wuhuM=lm(formula=y1~ lkld +dt +month + day +num +tianqi+nextWeek+ nextNum,data= wudata)");
            re.eval("save(wuhuM,file=\"" + outputUrl + "\")");
            //  System.out.println(re.eval("predict(wuhuM1,wudata[,0:9])"));
            //re.eval("write.table (predict(wuhuM1,wudata[,0:9]), file =\"E://ls.csv\", sep =\" \", row.names =FALSE, col.names =TRUE, quote =TRUE)");
            // re.eval("write.table(predict(wuhuM1,wudata[,0:9]), file =\""+outputUrl+"\", sep =\" \", row.names =FALSE, col.names =TRUE, quote =TRUE)");
        }else if(arithmetic==1){
            //randomForst
            re.eval("wudata=read.table(\""+inputUrl+"\",header=TRUE,sep=\",\",quote=\"\\\"\")");
            re.eval("library(\"randomForest\")");
            re.eval(" wuhuM=randomForest(formula=y2~ lkld +dt +month + day +num +tianqi+nextWeek+ nextNum,data= wudata,size=90)");
            re.eval("save(wuhuM,file=\"" + outputUrl + "\")");
        }

        return "success";
    }
    @ApiOperation(value="预测", notes="加载模型，进行预测")
    @RequestMapping(value="/dataPredict", method=RequestMethod.POST)
    @ResponseBody
    public Map predict(@RequestParam @ApiParam(required = true, value = "modelUrl") String modelUrl,@RequestParam @ApiParam(required = true, value = "inputUrl") String inputUrl, @RequestParam @ApiParam(required = true, value = "outputUrl") String outputUrl){
        Rengine re=Singleton.getSingleton();
        if (!re.waitForR()) {
            System.out.println("Cannot load R");
            System.out.println("R error：Cannot load R");
        }
        //打印变量
        String version = re.eval("R.version.string").asString();
        System.out.println(version);
        re.eval("load(\"" + modelUrl + "\")");
        re.eval("wudata=read.table(\""+inputUrl+"\",header=TRUE,sep=\",\",quote=\"\\\"\")");
        re.eval("write.table(predict(wuhuM,wudata[,0:9]), file =\""+outputUrl+"\", sep =\" \", row.names =FALSE, col.names =TRUE, quote =TRUE)");
        Map map=dataCleanService.resultReader(outputUrl);

        return map;
    }
    @ApiOperation(value="从hdfs下载", notes="从hdfs下载数据")
    @RequestMapping(value="/copyToLocal", method=RequestMethod.POST)
    @ResponseBody
    public String copyToLocal(@RequestParam @ApiParam(required = true, value = "inputUrl") String inputUrl, @RequestParam @ApiParam(required = true, value = "outputUrl") String outputUrl){
        dataCleanService.copyFromHdfs(inputUrl,outputUrl);
        return "success";
    }

}
