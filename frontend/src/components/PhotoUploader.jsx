import { Camera } from "lucide-react";

export default function PhotoUploader({ previewUrl, onChange }) {
  return (
    <label className={`photo-uploader${previewUrl ? " has-image" : ""}`}>
      <input
        className="photo-uploader__input"
        type="file"
        accept="image/png,image/jpeg,image/webp"
        onChange={onChange}
      />
      {previewUrl ? (
        <>
          <img className="photo-uploader__preview" src={previewUrl} alt="선택한 피부 사진 미리보기" />
          <span className="photo-uploader__change">사진 다시 선택하기</span>
        </>
      ) : (
        <span className="photo-uploader__empty">
          <span className="camera-bubble">
            <Camera size={28} strokeWidth={2.3} />
          </span>
          <strong>사진 촬영하기</strong>
          <small>밝은 곳에서 정면을 응시해주세요</small>
        </span>
      )}
    </label>
  );
}

