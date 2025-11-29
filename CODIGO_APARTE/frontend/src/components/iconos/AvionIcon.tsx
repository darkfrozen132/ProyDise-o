interface AvionIconProps {
  color: string;
}

export function AvionIcon({ color }: AvionIconProps) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      width="24"
      height="24"
      style={{ transform: 'rotate(90deg)' }}
    >
      <path
        fill={color}
        stroke="#1e293b"
        strokeWidth="1.5"
        d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"
      />
    </svg>
  );
}
