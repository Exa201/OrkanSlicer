#ifndef slic3r_CurledExtrusionUtils_hpp_
#define slic3r_CurledExtrusionUtils_hpp_

#include <vector>

namespace Slic3r {

class PrintRegion;

// Curl estimation is useful only when overhang speed and curled-perimeter slowdown are both active.
bool should_estimate_curled_extrusions(const std::vector<PrintRegion*> &regions);

} // namespace Slic3r

#endif // slic3r_CurledExtrusionUtils_hpp_
