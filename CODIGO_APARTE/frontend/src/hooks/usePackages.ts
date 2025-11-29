import { useCallback } from "react";
import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import {
  createPackage as createPackageRequest,
  getPackages,
  type Shipment,
} from "../services/packageService";

interface PackageFormData extends Omit<Shipment, "id"> {}

const shipmentsListKey = ["shipments", "list"] as const;
const shipmentDetailKey = (id: number | string) =>
  ["shipments", "detail", id] as const;

export function usePackages() {
  const queryClient = useQueryClient();

  const { data, isLoading, isFetching, refetch, error } = useQuery({
    queryKey: shipmentsListKey,
    queryFn: getPackages,
  });

  const createPackageMutation = useMutation({
    mutationFn: createPackageRequest,
    onSuccess: (created) => {
      queryClient.setQueryData<Shipment[]>(
        shipmentsListKey,
        (prev = []) => [...prev, created]
      );
      if (created.id !== undefined) {
        queryClient.setQueryData(shipmentDetailKey(created.id), created);
      }
    },
  });

  const createPackage = useCallback(
    (pkg: PackageFormData) => createPackageMutation.mutateAsync(pkg),
    [createPackageMutation]
  );

  const friendlyError =
    error || createPackageMutation.error
      ? "No se pudieron cargar o actualizar los paquetes"
      : null;

  return {
    packages: data ?? [],
    loading: isLoading,
    fetching: isFetching,
    error: friendlyError,
    queryError: error,
    mutationError: createPackageMutation.error,
    fetchPackages: refetch,
    createPackage,
    creating: createPackageMutation.isPending,
  };
}
