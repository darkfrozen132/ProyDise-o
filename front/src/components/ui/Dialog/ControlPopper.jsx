// MetricsPopper.jsx
import { Popper, Paper } from "@mui/material";

export default function ControlPopper({ open, anchorEl }) {
    return (
        <Popper
            open={open}
            anchorEl={anchorEl}
            placement="left"
            modifiers={[
                {
                    name: "offset",
                    options: { offset: [0, 5] }, // separación vertical
                },
            ]}
            sx={{ zIndex: 1300 }}
        >
            <Paper
                sx={{
                    borderRadius: 2,
                    width: 200,
                    paddingTop: -2,
                    boxShadow: 4,
                    backgroundColor: "rgba(255,255,255,0.8)",
                }}
            >
                <div className="stats-section" style = {{padding: "0px 15px 12px 15px"}}>

                </div>
            </Paper>
        </Popper>
    );
}
