// フォームの二重送信防止：submitボタンを押した瞬間にロックし、連打による重複リクエストを防ぐ。
// confirm()でキャンセルされた場合（onsubmitがfalseを返す＝event.defaultPreventedがtrueになる）は
// 実際には送信されないため、ボタンをロックしない。
document.addEventListener('submit', function (event) {
    if (event.defaultPrevented) {
        return;
    }

    var form = event.target;
    var submitButton = form.querySelector('button[type="submit"]');
    if (!submitButton || submitButton.disabled) {
        return;
    }

    submitButton.disabled = true;

    var loadingText = submitButton.dataset.loadingText;
    if (loadingText) {
        submitButton.textContent = loadingText;
    }
});
