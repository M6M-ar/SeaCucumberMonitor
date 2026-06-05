var loading

function showLoad() {
    return layer.msg('拼命执行中...', {icon: 16,shade: [0.5, '#f5f5f5'],scrollbar: false,offset: 'auto', time:100000});
}

function closeLoad(index) {
    layer.close(index);

}

function layer_tc(title,content) {
    layer.open({
        title: title
        ,content: content
    });

}