import { useEffect } from 'react';
import { useMap } from 'react-leaflet';

interface Props {
  isLoading: boolean;
}
function MapResizer({ isLoading }: Props) {
  const map = useMap();
  useEffect(() => {
    if (!isLoading) {
      const timer = setTimeout(() => {
        map.invalidateSize(true);
      }, 150);

      return () => clearTimeout(timer);
    }
  }, [map, isLoading]);

  return null;
}
export default MapResizer;