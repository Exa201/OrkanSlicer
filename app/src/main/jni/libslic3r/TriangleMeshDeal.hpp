#ifndef libslic3r_Timer_hpp_
#define libslic3r_Timer_hpp_

#include "TriangleMesh.hpp"

namespace Slic3r {
class TriangleMeshDeal
{
public:
    // Loop subdivision: every triangle is split into four and its vertices are smoothed.
    // `unsmoothed`, when supplied, receives the same topology before vertex smoothing.
    static TriangleMesh smooth_triangle_mesh(const TriangleMesh &mesh, bool &ok, TriangleMesh *unsmoothed = nullptr);
};
} // namespace Slic3r

#endif // libslic3r_Timer_hpp_
