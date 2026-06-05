function type_display(type) {
    $('#import').show()
    if(type=='本地声通'){
    	$('.id_set').hide()		
    }else{
    	$('.id_set').show()	
    }
    switch (type) {
        case CnstSmlBsOneFour:
            $('.base').show()
            $('.small-positioning').show()
            $('.one-six-positioning').hide()
            $('.beacon_setting').hide()
            $('.one-six_beacon_public').hide()
            $('.st_one_to_one_setting').hide()
            $('.one_six_network_port').hide()
            $('.st_tc').hide()
        //    $('.mini_beacon').hide()
            $('#read').addClass("small_read").removeClass('one_six_read beacon_read st_read mini_read');
            $('#write').addClass("small_write").removeClass('one_six_write beacon_write beacon_one_six_write st_write');
            $('#import').addClass("small_import").removeClass('one_six_import beacon_import st_import');
            break;
        case CnstSmlBsOneSix:
            $('.base').show()
            $('.one-six-positioning').show()
            $('.small-positioning').hide()
            $('.beacon_setting').hide()
            $('.one-six_beacon_public').show()
            $('.st_one_to_one_setting').hide()
            $('.one_six_network_port').hide()
            $('.st_tc').hide()
         //   $('.mini_beacon').hide()
            $('#read').addClass("one_six_read").removeClass('small_read beacon_read st_read mini_read');
            $('#write').addClass("one_six_write").removeClass('small_write beacon_write beacon_one_six_write st_write');
            $('#import').addClass("one_six_import").removeClass('small_import beacon_import st_import');
            break
        case CnstSmlBs12K:
            $('.base').show()
            $('.one-six-positioning').show()
            $('.small-positioning').hide()
            $('.beacon_setting').hide()
            $('.one-six_beacon_public').show()
            $('.st_one_to_one_setting').hide()
            $('.one_six_network_port').hide()
            $('.st_tc').hide()
         //   $('.mini_beacon').hide()
            $('#read').addClass("one_six_read").removeClass('small_read beacon_read st_read mini_read');
            $('#write').addClass("one_six_write").removeClass('small_write beacon_write beacon_one_six_write st_write');
            $('#import').addClass("one_six_import").removeClass('small_import beacon_import st_import');
            break
        case CnstBenDiShengTong:
            $('.base').show()
            $('.beacon_setting').show()
             $('.small-positioning').hide()
            $('.one-six-positioning').hide()
            $('.one-six_beacon_public').show()
            $('.one_six_network_port').show()
            $('.st_one_to_one_setting').hide()
            $('.st_tc').hide()
        //    $('.mini_beacon').hide()
            $("#small_id").attr("placeholder", "1~99");
            $('#read').addClass("beacon_read").removeClass('small_read one_six_read st_read mini_read');
            $('#write').addClass("beacon_one_six_write").removeClass('small_write one_six_write beacon_write st_write');
            $('#import').addClass("beacon_import").removeClass('small_import one_six_import st_import');
            break
        case CnstSmlTmOneOne:
            $('.base').show()
            $('.beacon_setting').show()
             $('.small-positioning').hide()
            $('.one-six-positioning').hide()
            $('.one-six_beacon_public').hide()
            $('.st_one_to_one_setting').hide()
            $('.one_six_network_port').hide()
            $('.st_tc').hide()
        //    $('.mini_beacon').hide()
            $("#small_id").attr("placeholder", "1~99");
            $('#read').addClass("beacon_read").removeClass('small_read one_six_read st_read mini_read');
            $('#write').addClass("beacon_write").removeClass('small_write one_six_write beacon_one_six_write st_write');
            $('#import').addClass("beacon_import").removeClass('small_import one_six_import st_import');
            break
        case '本地声通':
        	 $('.base').show()
			 $('.st_one_to_one_setting').show()			
            $('.beacon_setting').hide()
            $('.small-positioning').hide()
            $('.one-six-positioning').hide()
            $('.one-six_beacon_public').hide()
            $('.one_six_network_port').hide()
            $('.st_tc').show()
        //    $('.mini_beacon').hide()
            $("#small_id").attr("placeholder", "1~99");
            $('#read').addClass("st_read").removeClass('small_read one_six_read beacon_read mini_read');
            $('#write').addClass("st_write").removeClass('small_write one_six_write beacon_one_six_write beacon_write');
            $('#import').addClass("st_import").removeClass('small_import one_six_import beacon_import ');
            break
        case CnstSmlTmMini:
            $('.base').show()
            $('.beacon_setting').show()
            $('.small-positioning').hide()
            $('.one-six-positioning').hide()
            $('.one-six_beacon_public').show()
            $('.one_six_network_port').hide()
        //    $('.mini_beacon').show()
            $("#small_id").attr("placeholder", "1~99");
            $('#read').addClass("mini_read").removeClass('small_read one_six_read st_read beacon_read');
            $('#write').addClass("mini_write").removeClass('small_write one_six_write beacon_write st_write beacon_one_six_write');
            $('#import').addClass("beacon_import").removeClass('small_import one_six_import st_import');
            break
    }
}

function request_read(type,data) {
	 $('#small_product_type').text(data.DevProductName)
	 $('#small_sn').text(data.SnVer)
	 $('#small_version').text(data.SnSoft)
    switch (type) {
        case CnstSmlBsOneFour:
            $('#small_id').val(data.DevId)
            $('#small_salinity_id').val(data.Naci)
            $('#sound_value').val(data.BaseSpeed)
            $("input[name='small_env_radio'][value='"+data.BaseWorkEnv+"']").prop("checked",true)
            $("input[name='small_sound_radio'][value='"+data.BaseSpeedType+"']").prop("checked",true)
            $("input[name='small_model'][value='"+data.LocationMode+"']").prop("checked",true)
            if(data.LocationMode=='1'){
            	$("input[name='small_model'][value='0']").attr('disabled',"disabled");
				$("input[name='small_model'][value='1']").removeAttr("disabled");
				$('#small_pair_id').removeAttr("disabled")
           		$('#small_positioning_interval').removeAttr("disabled")
           		$('#small_syn_id').removeAttr("disabled")
				$('#small_range').attr('disabled',"disabled")
           		$('#small_polling_interval').attr('disabled',"disabled") 
				$('#small_polling_id').attr('disabled',"disabled")      
            }else{
            	$("input[name='small_model'][value='1']").attr('disabled',"disabled");
				$("input[name='small_model'][value='0']").removeAttr("disabled");
				$('#small_pair_id').attr('disabled',"disabled")
           		$('#small_positioning_interval').attr('disabled',"disabled")
           		$('#small_syn_id').attr('disabled',"disabled")
				$('#small_range').removeAttr("disabled")
           		$('#small_polling_interval').removeAttr("disabled")
				$('#small_polling_id').removeAttr("disabled")     
            }
            $('#small_pair_id').val(data.NumOfPeiDuiId)
            $('#small_positioning_interval').val(data.BaseLocTimeSet)
            $('#small_syn_id').val(data.BaseTimeSync)
            $('#small_range').val(data.WorkFangWei)
            $('#small_polling_interval').val(data.LunXunJianGe)           
            if(data.NumOfASkForId=='--'){
            	$('#small_polling_id').val('')
            }else{
            	$('#small_polling_id').val(data.NumOfASkForId)
            } 
            break
        case CnstSmlBsOneSix:
            $('#small_id').val(data.DevId)
            $('#one_six_depth').val(data.BaseWorkSetDeep)
            $('#one_six_sound').val(data.BaseSpeed)
            $('#one_six_range').val(data.WorkFangWei)
            $('#one_six_polling').val(data.LunXunJianGe)
            $('#one_six_beacons_id').val(data.NumOfASkForId)
            LocationMode=data.LocationMode          
            $('#shield').val(data.PingBiId)
            $('#send_power').val(data.FaSheGongLv)
            $('#public_baud').val(data.CuanKouBtl)
            $('#protection_interval').val(data.ShuJuKuanBaoFuJianGe)
            $('#one_six_model').val(data.FaSongMoShi)
            break
        case CnstSmlBs12K:
            $('#small_id').val(data.DevId)
            $('#one_six_depth').val(data.BaseWorkSetDeep)
            $('#one_six_sound').val(data.BaseSpeed)
            $('#one_six_range').val(data.WorkFangWei)
            $('#one_six_polling').val(data.LunXunJianGe)
            $('#one_six_beacons_id').val(data.NumOfASkForId)
            LocationMode=data.LocationMode          
            $('#shield').val(data.PingBiId)
            $('#send_power').val(data.FaSheGongLv)
            $('#public_baud').val(data.CuanKouBtl)
            $('#protection_interval').val(data.ShuJuKuanBaoFuJianGe)
            $('#one_six_model').val(data.FaSongMoShi)
            break
        case CnstSmlTmOneOne:
            $('#small_id').val(data.DevId)
            $('#base_id').val(data.DstId)
            $("input[name='beacon_model'][value='"+data.LocationMode+"']").prop("checked",true)   
            if(data.LocationMode=='1'){
            	$("input[name='beacon_model'][value='0']").attr('disabled',"disabled");
				$("input[name='beacon_model'][value='1']").removeAttr("disabled");
            }else{
            	$("input[name='beacon_model'][value='1']").attr('disabled',"disabled");
				$("input[name='beacon_model'][value='0']").removeAttr("disabled");			
            }
            $('#no_ops_time').val()
            break
        case CnstBenDiShengTong:
            $('#small_id').val(data.DevId)
            $('#base_id').val(data.DstId)
            $("input[name='beacon_model'][value='"+data.LocationMode+"']").prop("checked",true)
              if(data.LocationMode=='1'){
            	$("input[name='beacon_model'][value='0']").attr('disabled',"disabled");
				$("input[name='beacon_model'][value='1']").removeAttr("disabled");
            }else{
            	$("input[name='beacon_model'][value='1']").attr('disabled',"disabled");
				$("input[name='beacon_model'][value='0']").removeAttr("disabled");			
            }
            if(data.PingBiId=='OFF' || data.PingBiId=='ON'){
            	if(data.PingBiId=='OFF'){
					$('#shield').val(1)
				}else{
					$('#shield').val(0)
				}
            }else{
            	$('#shield').val(data.PingBiId)
            }            
            $('#send_power').val(data.FaSheGongLv)
            $('#public_baud').val(data.CuanKouBtl)
            $('#protection_interval').val(data.ShuJuKuanBaoFuJianGe)
            break
        case '本地声通':       
            $('#st_local_id').val(data.DevId)
            $('#st_sound_id').val(data.DstId)
              if(data.PingBiId=="OFF" || data.PingBiId=="ON"){
            	if(data.PingBiId=="OFF"){
					$('#st_shield').val(0)
				}
				if(data.PingBiId=="ON"){
					$('#st_shield').val(1)
				}
            }else{
            	$('#st_shield').val(data.PingBiId)
            }  
            $('#st_power').val(data.FaSheGongLv)
            $('#st_inteval').val(data.ShuJuKuanBaoFuJianGe)
            $('#st_baud').val(data.CuanKouBtl)
            break
        case CnstSmlTmMini:
            $('#small_id').val(data.DevId)
            $('#base_id').val(data.DstId)
            $("input[name='beacon_model'][value='"+data.LocationMode+"']").prop("checked",true)
            if(data.LocationMode=='1'){
            	$("input[name='beacon_model'][value='0']").attr('disabled',"disabled");
				$("input[name='beacon_model'][value='1']").removeAttr("disabled");
            }else{
            	$("input[name='beacon_model'][value='1']").attr('disabled',"disabled");
				$("input[name='beacon_model'][value='0']").removeAttr("disabled");			
            }
            if(data.SlpAuto==""){
	$("input[name='xiumiankaiguan'][value='1']").removeAttr('checked');
	$("input[name='xiumiankaiguan'][value='0']").removeAttr('checked');
	}else{
	   $("input[name='xiumiankaiguan'][value='"+data.SlpAuto+"']").prop("checked",true)
	}
            $('#no_ops_time').val(data.SlpTime)
            $('#shield').val(data.PingBiId)
            $('#send_power').val(data.FaSheGongLv)
            $('#public_baud').val(data.CuanKouBtl)
            $('#protection_interval').val(data.ShuJuKuanBaoFuJianGe)
            $('#system_time').prop("checked",data.checked);
            break

    }
}

function Validator(type) {
    var info=''
    if(type !='st_write'){
    var small_id=$('#small_id').val()
    if(small_id==''){
        layer_tc('提示','设备ID不能为空！')
        return false
    }
    }
    var dev_type =$('#small_product_type').text()
    switch (type) {
        case 'small_write':
            if(small_id<1 || small_id>255){
                layer_tc('提示','设备ID范围为1~255，请重新输入！')
                return false
            }
            var small_salinity_id =$('#small_salinity_id').val()
            var sound_value = $('#sound_value').val()
            var small_env_radio =$("input[name='small_env_radio']:checked").val()
            var small_sound_radio =$("input[name='small_sound_radio']:checked").val()
            var small_model =$("input[name='small_model']:checked").val()
            var small_pair_id = $('#small_pair_id').val()
            var small_positioning_interval = $('#small_positioning_interval').val()
            var small_syn_id = $('#small_syn_id').val()
            var small_range = $('#small_range').val()
            var small_polling_interval = $('#small_polling_interval').val()
            var small_polling_id = $('#small_polling_id').val()
            

            if(small_env_radio==undefined){
                layer_tc('提示','请选择使用环境！')
                return false
            }else if(small_env_radio==0){console.log(small_salinity_id)
                if(small_salinity_id==''){
                    layer_tc('提示','请输入盐度值！')
                    return false
                }else if(small_salinity_id<0 || small_salinity_id>255){
                    layer_tc('提示','盐度范围为0~255，请重新输入！')
                    return false
                }
            }
            if(small_sound_radio==undefined){
                layer_tc('提示','请选择水声声速！')
                return false
            }else if(small_sound_radio=='OFF'){
                if(sound_value==''){
                    layer_tc('提示','请输入声速值！')
                    return false
                }else if(sound_value<300 || sound_value>1700){
                    layer_tc('提示','声速范围为300~1700，请重新输入！')
                    return false
                }
            }
            if(small_model==undefined){
                layer_tc('提示','请选择模式！')
                return false
            }else if(small_model==1){
                if(small_pair_id==''){
                    layer_tc('提示','请输入配对信标ID！')
                    return false
                }else if(small_pair_id<0 || small_pair_id>99){
                    layer_tc('提示','配对信标ID范围为0~99，请重新输入！')
                    return false
                }
                if(small_positioning_interval==''){
                    layer_tc('提示','请输入定位时间间隔！')
                    return false
                }else if(small_positioning_interval<2 || small_positioning_interval>255){
                    layer_tc('提示','定位时间间隔范围为2~255，请重新输入！')
                    return false
                }
                if(small_syn_id==''){
                    layer_tc('提示','请输入同步时间间隔！')
                    return false
                }else if(small_syn_id<30 || small_syn_id>255){
                    layer_tc('提示','同步时间间隔范围为30~255，请重新输入！')
                    return false
                }
            }else if(small_model==0){
                if(small_range==''){
                    layer_tc('提示','请输入工作范围！')
                    return false
                }else if(small_range<10 || small_range>1000){
                    layer_tc('提示','工作范围为10~1000，请重新输入！')
                    return false
                }
                if(small_polling_interval==''){
                    layer_tc('提示','请输入轮询间隔！')
                    return false
                }else if(small_polling_interval<0 || small_polling_interval>65535){
                    layer_tc('提示','轮询间隔范围为0~65535，请重新输入！')
                    return false
                }
                if(small_polling_id==''){
                    layer_tc('提示','请输入轮询信标ID！')
                    return false
                }
                var arr=[]
                var small_polling_id =small_polling_id.replace(/\；/g,"\;")
                arr = small_polling_id.split(";")
                var re =  /^\+?[1-9][0-9]*$/;
                for (var i=0;i<arr.length ;i++ ){
                    if (!re.test(Number(arr[i])) ) {
                        layer_tc('提示','输入格式不正确，请重新输入！')
                        return false
                    }else {
                        if(Number(arr[i])>255 || Number(arr[i])<1){
                            layer_tc('提示','轮询信标ID范围为1~255，请重新输入！')
                            return false
                        }
                    }
                }
       
            }
            info={'DevStgType':dev_type,'DevId':small_id,'BaseWorkEnv':small_env_radio,'Naci':small_salinity_id,'BaseSpeedType':small_sound_radio,
                'BaseSpeed':sound_value,'LocationMode':small_model,'NumOfPeiDuiId':small_pair_id,'BaseLocTimeSet':small_positioning_interval,
                'BaseTimeSync':small_syn_id,'WorkFangWei':small_range,'LunXunJianGe':small_polling_interval,'NumOfASkForId':small_polling_id,'DevNumType':DevType}
            return info
            break
        case 'one_six_write':
            if(small_id<1 || small_id>255){
                layer_tc('提示','设备ID范围为1~255！')
                return false
            }
            var one_six_depth =$('#one_six_depth').val()
            var one_six_sound =$('#one_six_sound').val()
            var one_six_range =$('#one_six_range').val()
            var one_six_polling =$('#one_six_polling').val()
            var one_six_beacons_id =$('#one_six_beacons_id').val()
            var shield =$('#shield').val()
            var send_power =$('#send_power').val()
            var public_baud  =$('#public_baud').val()
            var protection_interval =$('#protection_interval').val()
            var one_six_model =$('#one_six_model').val()
     
            if(one_six_depth==''){
                layer_tc('提示','请输入工作深度！')
                return false
            }
            if(one_six_sound==''){
                layer_tc('提示','请输入声速！')
                return false
            }else if(one_six_sound<300 || one_six_sound>1700){
                layer_tc('提示','声速范围为300~1700，请重新输入！')
                return false
            }
            if(one_six_range==''){
                layer_tc('提示','请输入工作范围！')
                return false
            }else if(one_six_range<1 || one_six_range>3500){
                layer_tc('提示','工作范围为1~3500，请重新输入！')
                return false
            }
            if(one_six_polling==''){
                layer_tc('提示','请输入轮询间隔！')
                return false
            }else if(one_six_polling<0 || one_six_polling>65535){
                layer_tc('提示','轮询间隔为0~65535，请重新输入！')
                return false
            }
            if(one_six_beacons_id==''){
                layer_tc('提示','请输入轮询信标ID！')
                return false
            }
            var arr=[]
            arr = one_six_beacons_id.split(";")
            var re = /^\+?[1-9][0-9]*$/;
            for (var i=0;i<arr.length ;i++ ){
                if (!re.test(Number(arr[i]))) {
                    layer_tc('提示','输入格式不正确，请重新输入！')
                    return false
                }else {
                    if(Number(arr[i])>255 || Number(arr[i])<1){
                        layer_tc('提示','轮询信标ID范围为1~255，请重新输入！')
                        return false
                    }
                }
            }
     
            info={'DevStgType':dev_type,'DevId':small_id,'BaseWorkSetDeep':one_six_depth,'BaseSpeed':one_six_sound,'WorkFangWei':one_six_range,'LunXunJianGe':one_six_polling,'NumOfASkForId':one_six_beacons_id,'DevNumType':DevType,
                'LocationMode':LocationMode,'PingBiId':shield,'FaSheGongLv':send_power,'CuanKouBtl':public_baud,'ShuJuKuanBaoFuJianGe':protection_interval,'FaSongMoShi':one_six_model}
            return info
            break
        case 'beacon_write':
            if(small_id<1 || small_id>99){
                layer_tc('提示','设备ID范围为1~99，请重新输入！')
                return false
            }
            var base_id =$('#base_id').val()
            var beacon_model =$("input[name='beacon_model']:checked").val()
            if(base_id==''){
                layer_tc('提示','基站ID不能为空！')
                return false
            }else if(base_id<1 || base_id>255){
                layer_tc('提示','基站ID范围为1~255，请重新输入！')
                return false
            }
            if(beacon_model==undefined){
                layer_tc('提示','请选择定位模式！')
                return false
            }
            var no_ops_time =$('#no_ops_time').val()
            info ={'DevStgType':dev_type,'DevId':small_id,'DstId':base_id,'LocationMode':beacon_model,'DevNumType':DevType}
            return info
            break
        case 'beacon_one_six_write':
        	      if(small_id<1 || small_id>99){
                layer_tc('提示','设备ID范围为1~99，请重新输入！')
                return false
            }
            var base_id =$('#base_id').val()
            var beacon_model =$("input[name='beacon_model']:checked").val()
            var shield =$('#shield').val()
            var send_power =$('#send_power').val()
            var public_baud  =$('#public_baud').val()
            var protection_interval =$('#protection_interval').val()    
            if(base_id==''){
                layer_tc('提示','基站ID不能为空！')
                return false
            }else if(base_id<100 || base_id>199){
                layer_tc('提示','基站ID范围为100~199，请重新输入！')
                return false
            }
            if(beacon_model==undefined){
                layer_tc('提示','请选择定位模式！')
                return false
            }
            
            info ={'DevStgType':dev_type,'DevId':small_id,'DstId':base_id,'LocationMode':beacon_model,'DevNumType':DevType,
                'PingBiId':shield,'FaSheGongLv':send_power,'CuanKouBtl':public_baud,'ShuJuKuanBaoFuJianGe':protection_interval}
            return info
            break
        case 'st_write':
        	 var st_local_id =$('#st_local_id').val()
            var st_sound_id =$('#st_sound_id').val()
            var st_shield =$('#st_shield').val()
            if(st_shield=='1'){
            	st_shield='ON'
            }else{
            	st_shield='OFF'
            }
            var st_power =$('#st_power').val()
            var st_inteval  =$('#st_inteval').val()
            var st_baud  =$('#st_baud ').val()    
            if(st_local_id==''){
                layer_tc('提示','本机ID地址不能为空！')
                return false
            }else if(base_id<101 || base_id>199){
                layer_tc('提示','本机ID地址范围为1~255，请重新输入！')
                return false
            }
            if(st_local_id==''){
                layer_tc('提示','目标声通ID地址不能为空！')
                return false
            }else if(base_id<101 || base_id>199){
                layer_tc('提示','目标声通ID地址范围为1~255，请重新输入！')
                return false
            }
            info ={'DevStgType':dev_type,'DevId':st_local_id,'DstId':st_sound_id,'DevNumType':DevType,
                'PingBiId':st_shield,'FaSheGongLv':st_power,'CuanKouBtl':st_baud,'ShuJuKuanBaoFuJianGe':st_inteval}
            return info
            break
        case 'mini_write':
            if(small_id<1 || small_id>99){
                layer_tc('提示','设备ID范围为1~99，请重新输入！')
                return false
            }
            var base_id =$('#base_id').val()
            var beacon_model =$("input[name='beacon_model']:checked").val()
            var shield =$('#shield').val()
            var send_power =$('#send_power').val()
            var public_baud  =$('#public_baud').val()
            var protection_interval =$('#protection_interval').val()
            if(base_id==''){
                layer_tc('提示','基站ID不能为空！')
                return false
            }else if(base_id<100 || base_id>199){
                layer_tc('提示','基站ID范围为100~199，请重新输入！')
                return false
            }
            if(beacon_model==undefined){
                layer_tc('提示','请选择定位模式！')
                return false
            }
            var system_time =''
            if($('#system_time').is(':checked')) {
                system_time=1
            }else {
                system_time=0
            }
            var xiumiankaiguan =$("input[name='xiumiankaiguan']:checked").val()
            var no_ops_time =$('#no_ops_time').val()
            
            info ={'DevStgType':dev_type,'DevId':small_id,'DstId':base_id,'LocationMode':beacon_model,'DevNumType':DevType,
                'PingBiId':shield,'FaSheGongLv':send_power,'CuanKouBtl':public_baud,'ShuJuKuanBaoFuJianGe':protection_interval,
                'SlpTime':no_ops_time,'SlpAuto':xiumiankaiguan}
            return info
            break
        
    }
}

function import_read(type) {
    switch (type) {
        case 'small_import':
            $('#small_id').val('101')
            $('#small_salinity_id').val('35')
            $('#sound_value').val('1530')
            $("input[name='small_env_radio'][value='0']").prop("checked",true)
            $("input[name='small_sound_radio'][value='ON']").prop("checked",true)
            $("input[name='small_model'][value='1']").prop("checked",true)
            $('#small_pair_id').val('1')
            $('#small_positioning_interval').val('2')
            $('#small_syn_id').val('120')
            $('#small_range').val('1000')
            $('#small_polling_interval').val('0')
            $('#small_polling_id').val('1')
            break
        case 'one_six_import':
            $('#small_id').val('101')
            $('#one_six_depth').val('3')
            $('#one_six_sound').val('1500')
            $('#one_six_range').val('1000')
            $('#one_six_polling').val('0')
            $('#one_six_beacons_id').val('1')
            break
        case 'beacon_import':
            $('#small_id').val('1')
            $('#base_id').val('101')
            $("input[name='beacon_model'][value='1']").prop("checked",true)
            $("input[name='xiumiankaiguan'][value='1']").prop("checked",true)
            $('#no_ops_time').val('30')
            break
        case 'st_import':
            $('#st_inteval').val('150')
            $('#st_shield').val('0')
            break
    }
}

function remove_class() {
    $('#read').removeClass('small_read one_six_read beacon_read st_read mini_read');
    $('#write').removeClass('small_write one_six_write beacon_write st_write mini_write');
    $('#import').removeClass('small_import one_six_import beacon_import st_import');
    $('#open_com').show()
    $('#close_com').hide()
    $('#import').hide()
  //  $('.mini_beacon').hide()
}

function close_all_area() {
    $('.base').hide()
    $('.small-positioning').hide()
    $('.one-six-positioning').hide()
    $('.beacon_setting').hide()
    $('.one-six_beacon_public').hide()
    $('.one_six_network_port').hide()
    $('.st_one_to_one_setting').hide()
    $('.st_tc').hide()
   // $('.mini_beacon').hide()
    
}