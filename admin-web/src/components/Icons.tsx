import type { SVGProps } from "react";

type IconName = "report" | "action" | "audit" | "search" | "refresh" | "arrow" | "shield" | "logout";

export default function Icon({ name, ...props }: SVGProps<SVGSVGElement> & { name: IconName }) {
  const common = { fill: "none", stroke: "currentColor", strokeWidth: 1.8, strokeLinecap: "round" as const, strokeLinejoin: "round" as const };
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" {...props}>
      {name === "report" && <><path {...common} d="M12 2.5 19 6v6.7c0 4.2-2.6 7-7 8.8-4.4-1.8-7-4.6-7-8.8V6l7-3.5Z"/><path {...common} d="M12 7v6"/><circle cx="12" cy="16.5" r="1" fill="currentColor"/></>}
      {name === "action" && <><circle {...common} cx="12" cy="12" r="9"/><path {...common} d="M12 7v10M7 12h10"/></>}
      {name === "audit" && <><rect {...common} x="5" y="3" width="14" height="18" rx="1"/><path {...common} d="M9 8h6M9 12h6M9 16h6"/></>}
      {name === "search" && <><circle {...common} cx="10.5" cy="10.5" r="6.5"/><path {...common} d="m15.5 15.5 5 5"/></>}
      {name === "refresh" && <><path {...common} d="M20 6v5h-5"/><path {...common} d="M18.2 16A8 8 0 1 1 20 11"/></>}
      {name === "arrow" && <><path {...common} d="M5 12h14M14 7l5 5-5 5"/></>}
      {name === "shield" && <><path {...common} d="M12 2.5 19 6v6.7c0 4.2-2.6 7-7 8.8-4.4-1.8-7-4.6-7-8.8V6l7-3.5Z"/><path {...common} d="m8.5 12 2.2 2.2 4.8-5"/></>}
      {name === "logout" && <><path {...common} d="M10 4H5v16h5M14 8l4 4-4 4M8 12h10"/></>}
    </svg>
  );
}
