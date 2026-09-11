// 出品フォームの写真アップロード欄：選択した画像ファイルをその場でプレビュー表示する。
function previewPhoto(event) {
    var file = event.target.files && event.target.files[0];
    if (!file) {
        return;
    }

    var preview = document.getElementById('photoPreview');
    var placeholder = document.getElementById('photoUploadPlaceholder');
    var reader = new FileReader();
    reader.onload = function (loadEvent) {
        preview.src = loadEvent.target.result;
        preview.hidden = false;
        placeholder.hidden = true;
    };
    reader.readAsDataURL(file);
}
